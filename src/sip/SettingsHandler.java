package sip;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsHandler {
	// options
	private static final String CONFIG_FILE_OPT = "-c";
	private static final String SIP_USER_OPT = "-user";
	private static final String HTTP_ADDR_OPT = "-http";

	private static final String[] optionList = {CONFIG_FILE_OPT, 
												SIP_USER_OPT, 
												HTTP_ADDR_OPT};

	// properties
	public static final String DEFAULT_WAV = "default_message_wav";
	public static final String DEFAULT_WAV_VAL = "default.wav";

	public static final String DEFAULT_TEXT = "default_message_text";
	public static final String DEFAULT_TEXT_VAL = "Hello. This is my own answering machine. You have no new messages.";

	public static final String CURRENT_WAV = "message_wav";
	public static final String CURRENT_WAV_VAL = "currentmessage.wav";

	public static final String SIP_INTERFACE = "sip_interface";
	public static final String SIP_INTERFACE_VAL = "0.0.0.0";

	public static final String SIP_PORT = "sip_port";
	public static final String SIP_PORT_VAL = "5060";

	public static final String SIP_USER = "sip_user";
	public static final String SIP_USER_VAL = "sipspeaker";

	public static final String HTTP_INTERFACE = "http_interface";
	public static final String HTTP_INTERFACE_VAL = "0.0.0.0";

	public static final String HTTP_PORT = "http_port";
	public static final String HTTP_PORT_VAL = "80";

	// default config file
	public static final String DEFAULT_CONFIG_FILE = "sipspeaker.cfg";

	public Properties parse(String[] args) throws Exception {
		try {
			// check the number of arguments
			if (args.length != 0 && args.length % 2 == 1) {
				throw new Exception("Error: Syntax error in parameters");
			}

			// read the configuration from command line
			Map<String, String> parameters = new HashMap<String, String>();

			for (int i = 0; i < args.length; i += 2) {
				parameters.put(args[i], args[i+1]);
				if (!Arrays.asList(optionList).contains(args[i])) {
					throw new Exception("Error: Non-existent options: " + args[i]);
				}
			}

			String configFile = DEFAULT_CONFIG_FILE;
			if (parameters.get(CONFIG_FILE_OPT) != null) {
				configFile = parameters.get(CONFIG_FILE_OPT);
			}
			String sipUser = parameters.get(SIP_USER_OPT);
			String httpAddr = parameters.get(HTTP_ADDR_OPT);

			// read settings from the configuration file if any
			Properties props = readSettingsFromFile(configFile);

			// merge with the default configuration
			Properties defaultProps = getDefaultSettings();
			Enumeration<?> e = defaultProps.propertyNames();
			while (e.hasMoreElements()) {
				String opt = (String) e.nextElement();
				String defaultValue = defaultProps.getProperty(opt);

				String curValue = props.getProperty(opt).trim();
				if (curValue == null || curValue.length() == 0) {
					props.setProperty(opt, defaultValue);
				}
			}

			// parse the sip user option
			parseSipUser(sipUser, props);

			// parse the http address option
			parseHttpAddress(httpAddr, props);

			Log.print("Configuration: ");
			e = props.propertyNames();
			while (e.hasMoreElements()) {
				String opt = (String) e.nextElement();
				String curValue = props.getProperty(opt).trim();

				System.out.println(opt + ": " + curValue);
			}
			System.out.println();

			// remove redundant space
			trim(props);

			// validate the configuration
			validate(props);

			return props;
		} catch (Exception e) {
			throw e;
		}
	}

	private void trim(Properties props) {
		Enumeration<?> e = props.propertyNames();
		while (e.hasMoreElements()) {
			String opt = (String) e.nextElement();
			props.setProperty(opt, props.getProperty(opt).trim());
		}
	}

	/*!
	 * Validate the properties
	 */
	private void validate(Properties props) throws Exception {
		if (props.getProperty(DEFAULT_WAV).indexOf(".wav") <= 0) {
			throw new Exception("Error: Name of default message sound file is invalid");
		}

		if (props.getProperty(CURRENT_WAV).indexOf(".wav") <= 0) {
			throw new Exception("Error: Name of current message sound file is invalid");
		}

		if (!isUsername(props.getProperty(SIP_USER))) {
			throw new Exception("Error: Invalid sip username");
		}

		String sipInterface = props.getProperty(SIP_INTERFACE);
		if (!isAddress(sipInterface) && !isLocalhost(sipInterface)) {
			InetAddress addr = DNSHandler.resolveDomain(sipInterface);
			if (addr == null) {
				throw new Exception("Error: SIP interface doesn't exist");
			} else if (!isLocalInterface(addr)){
				throw new Exception("Error: The specified SIP interface is not a local interface");
			}
		}

		String httpInterface = props.getProperty(HTTP_INTERFACE);
		if (!isAddress(httpInterface) && !isLocalhost(httpInterface)) {
			InetAddress addr = DNSHandler.resolveDomain(httpInterface);
			if (addr == null) {
				throw new Exception("Error: HTTP interface doesn't exist");
			} else if (!isLocalInterface(addr)){
				throw new Exception("Error: The specified HTTP interface is not a local interface");
			}
		}
	}

	/*!
	 * Parse the sip user option
	 */
	private void parseSipUser(String sipUser, Properties props) throws Exception {
		if (sipUser == null || props == null) {
			return;
		}

		String[] patternStr = {"^(\\w+)@([a-z0-9\\.-]+):(\\d+)$",	// sipuser@interface:port
								"^(\\w+)@([a-z0-9\\.-]+)$"};		// sipuser@interface

		try {
			for (int i = 0; i < patternStr.length; i++) {
				Pattern pattern = Pattern.compile(patternStr[i]);
				Matcher matcher = pattern.matcher(sipUser);	
				if (matcher.find()) {
					props.setProperty(SIP_USER, matcher.group(1));
					props.setProperty(SIP_INTERFACE, matcher.group(2));
					if (matcher.groupCount() == 3) {
						props.setProperty(SIP_PORT, matcher.group(3));	
					}
					return;
				}	
			}
			
			throw new Exception();	
		} catch (Exception e) {
			throw new Exception("Error: Illegal value for option: " + SIP_USER_OPT);
		}
	}

	/*!
	 * Parse the http address option
	 */
	private void parseHttpAddress(String httpAddr, Properties props) throws Exception {
		if (httpAddr == null || props == null) {
			return;
		}

		// port only
		if (isInteger(httpAddr)) {
			props.setProperty(HTTP_PORT, Integer.parseInt(httpAddr)+"");
			return;
		}

		String[] patternStr = {"^([a-z0-9\\.-]+):(\\d+)$", // interface:port
								"^([a-z0-9\\.-]+)$"};		// interface only

		try {
			for (int i = 0; i < patternStr.length; i++) {
				Pattern pattern = Pattern.compile(patternStr[i]);
				Matcher matcher = pattern.matcher(httpAddr);	
				if (matcher.find()) {
					props.setProperty(HTTP_INTERFACE, matcher.group(1));
					if (matcher.groupCount() == 2) {
						props.setProperty(HTTP_PORT, matcher.group(2));	
					}
					return;
				}	
			}
			
			throw new Exception();	
		} catch (Exception e) {
			throw new Exception("Error: Illegal value for option: " + HTTP_ADDR_OPT);
		}
	}

	/*!
	 * Read the settings from configuration file
	 */
	private Properties readSettingsFromFile(String filename) throws Exception {
		// check if the file exists
		File file = new File(filename);
		if (!file.exists()) {
			throw new Exception("The configuration file " + filename + " does not exist");
		}

		Properties props = new Properties();

		try {
			InputStream input = new FileInputStream(filename);
			props.load(input);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return props;
	}

	/*!
	 * Return the default settings
	 */
	private Properties getDefaultSettings() {
		Properties props = new Properties();
		props.setProperty(DEFAULT_WAV, DEFAULT_WAV_VAL);
		props.setProperty(DEFAULT_TEXT, DEFAULT_TEXT_VAL);
		props.setProperty(CURRENT_WAV, CURRENT_WAV_VAL);
		props.setProperty(SIP_INTERFACE, SIP_INTERFACE_VAL);
		props.setProperty(SIP_USER, SIP_USER_VAL);
		props.setProperty(SIP_PORT, SIP_PORT_VAL);
		props.setProperty(HTTP_INTERFACE, HTTP_INTERFACE_VAL);
		props.setProperty(HTTP_PORT, HTTP_PORT_VAL);
		return props;
	}

	/*!
	 * Check if a string is in username's format
	 */
	private boolean isUsername(String s) {
		Pattern pattern = Pattern.compile("^[\\w\\.]+$");
		Matcher matcher = pattern.matcher(s);
		return matcher.find();
	}

	/*!
	 * [isLocalhost description]
	 */
	private boolean isLocalhost(String s) {
		return s.equals("localhost");
	}

	/*!
	 * Check if a string is in address pattern
	 */
	private boolean isAddress(String s) {
		Pattern pattern = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
		Matcher matcher = pattern.matcher(s);	
		return matcher.find();
	}

	private boolean isLocalInterface(InetAddress addr) {
		try {
			return (NetworkInterface.getByInetAddress(addr) != null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/*!
	 * Check if a string representing an integer
	 */
	private boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {

		}
		return false;
	}
}
