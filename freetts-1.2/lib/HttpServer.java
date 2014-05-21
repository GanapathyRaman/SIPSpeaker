import java.net.*; 
import java.io.*; 
import java.util.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.Timer;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

class HttpRequest implements Runnable{ 
	private static final String HTML_FOLDER = "html/";
	private static final String AUDIO_FOLDER = "audio/";
	private static final String HTML_INDEX = HTML_FOLDER + "index.htm";
	private static final String HTML_VOICE = HTML_FOLDER + "voice.htm";
	private static final String HTML_RESULT = HTML_FOLDER + "result.htm";
	private static final String HTML_404 = HTML_FOLDER + "404.htm";
	private static final String HTML_EMPTY = HTML_FOLDER + "empty.htm";

	private static final String TEXT_TO_AUDIO = "result";
	private static final String SAVED_VOICE = "voice";
	private static final String DELETE_VOICE = "delete";

	private static final String TEXT_FILE = "currentmessage.txt";
	private static final String AUDIO_FILE = "currentmessage.wav";
	private static final String DEFAULT_MSG = "Welcome to SIP Speaker. This is my own answering machine. You have no new messages.";
	private static final String DEFAULT_TEXT_FILE = "default.txt";
	private static final String DEFAULT_AUDIO_FILE = "default.wav";



	private Socket mClientConn; 

	public HttpRequest(Socket clientConn) throws Exception { 
		this.mClientConn = clientConn; 
	} 

	public void run() { 
		try {
				BufferedReader in = new BufferedReader(new InputStreamReader(mClientConn.getInputStream()));
				BufferedOutputStream out = new BufferedOutputStream(mClientConn.getOutputStream()); 

				String request = in.readLine().trim(); 
				Log.print("Request: " + request);

				StringTokenizer st = new StringTokenizer(request); 
				String requestType = st.nextToken(); 
				String action = st.nextToken().substring(1); 

				// return index.htm
				if (requestType.equals("GET") && (action.equals("") || action.equals("index"))) { 
					returnHTMLFile(HTML_INDEX, 200, out);	
				// compose email
				} else if (requestType.equals("POST") && action.equals(TEXT_TO_AUDIO)) {
					storeIntoFile(in, out);
				// show saved message
				} else if (requestType.equals("GET") && action.equals(SAVED_VOICE)) {
					existingMessage(out);
				// delete existing message
				} else if (requestType.equals("GET") && action.equals(DELETE_VOICE)) {
					deleteMessage(out);
				// action undefined
				} else {
					returnHTMLFile(HTML_404, 404, out);
				}
				out.close(); 
			} 
			catch (Exception e) {
				e.printStackTrace();
	    }

		// close connection
		try {
			mClientConn.close(); 	
		} catch (IOException e) {
			e.printStackTrace();
		}
	} 

	public void check() {

		File defaultAudioFile = new File(AUDIO_FOLDER + DEFAULT_AUDIO_FILE);
		File defaultTextFile = new File(AUDIO_FOLDER + DEFAULT_TEXT_FILE);

		Log.print("Checking for Default File...");

		if (defaultAudioFile.exists() && defaultTextFile.exists()){
				Log.print("Default File exist...");
		 		Log.print("...Skipping default file creation part");
		 		return;
		} else {
		 	Log.print("Default File Not found...");
		 	Log.print("Creating default file...");
		 	dataInAudioFormat(DEFAULT_MSG, DEFAULT_AUDIO_FILE, DEFAULT_TEXT_FILE);
		 	Log.print("Default File has be successfully created");
		}
	} 

	/*
	* Writing message in .WAV format
	*/
	public void dataInAudioFormat(String message, String audiofile, String textfile)
	{

		//Log.print("This is the message: " + message);

		try{
          	File file = new File(AUDIO_FOLDER + textfile);
          	BufferedWriter output = new BufferedWriter(new FileWriter(file));
          	output.write(message);
          	output.close();
          	Log.print("Saving the message to " + AUDIO_FOLDER + textfile);
      	} catch (Exception e){	//Catch exception if any
      		Log.print("Error: " + e.getMessage());
    	}

    	try{
			System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");

        	Voice voice;
        	VoiceManager vm = VoiceManager.getInstance();
        	voice = vm.getVoice("kevin16");
        	voice.allocate();

        	String baseName = audiofile.substring(0, audiofile.toLowerCase().indexOf(".wav"));
        	SingleFileAudioPlayer sfap = new SingleFileAudioPlayer(AUDIO_FOLDER + 
        								 baseName, javax.sound.sampled.AudioFileFormat.Type.WAVE);
        	voice.setAudioPlayer(sfap);
        	voice.speak(message);

        	sfap.close();

        	voice.deallocate();
		} catch (Exception e){	//Catch exception if any
      		Log.print("Error: " + e.getMessage());
    	}
    }
	/**
	 * Getting the data from POST data
	 */
	public void storeIntoFile(BufferedReader in, 
									BufferedOutputStream out) throws Exception{
		Log.print("Receiving request to store the voice data...");
		String message = null;
		int statusCode = 200;

		try {
			String data = parsePOSTData(in);
			
			if (data == "") {
				returnHTMLFile(HTML_EMPTY, 200, out);
				message = "Invalid data";
				return;
			} else {
				message = data;
				dataInAudioFormat(message, AUDIO_FILE, TEXT_FILE);
			}

			String resHeader = createHeader(statusCode, 5);

			Log.print("Storing the following message into the file: \n" + message);
			out.write(resHeader.getBytes());
			parseHTMLFile(HTML_RESULT, out);

		} catch (Exception e) {
			e.printStackTrace();
			Log.print("Empty data");
			message = "Invalid data";
		}
	}


	public void existingMessage(BufferedOutputStream out) {
		try {
			// write header
			Log.print("Received request for reading the existing file...");
			String message = "";
			String resHeader = createHeader(200, 5);
			out.write(resHeader.getBytes());
			parseHTMLFile(HTML_VOICE, out);

			File currentTextFile = new File(AUDIO_FOLDER + TEXT_FILE);
			File currentAudioFile = new File(AUDIO_FOLDER + AUDIO_FILE);

			String textfile = DEFAULT_TEXT_FILE;
			String audiofile = DEFAULT_AUDIO_FILE;

			if (currentTextFile.exists()){
				if(currentAudioFile.exists()){
					textfile = TEXT_FILE;
					audiofile = AUDIO_FILE;
				} else {
					Log.print("Error: Missing 'Current Message' Audio File");
					Log.print("Alert: System will be using 'Default Message' Audio File");
				}
			} else {
				Log.print("Error: Missing 'Current Message' Text File");
				Log.print("Alert: System will be using 'Default Message' Text File");
			}

        	try{
            	InputStream ips=new FileInputStream(AUDIO_FOLDER + textfile);
            	InputStreamReader ipsr=new InputStreamReader(ips);
            	BufferedReader br=new BufferedReader(ipsr);
            	String line;
            	while ((line=br.readLine())!=null){
                	message+=line;
            	}
            	Log.print("Reading the existing message: \n" + message);
            	br.close();
        	}
        	catch (Exception e){
            	Log.print("Error: " + e.toString());
        	}
        	out.write(("</br>" + message + "</p></th>").getBytes());
        	out.write(("<th style='width: 650px' >").getBytes());
        	out.write(("<h3 align='center'>To save a new message").getBytes());
    		out.write(("<a href = 'index'><h3>Click Here</a></h3><br/>").getBytes());
    		out.write(("<h3 align='center'>To Delete the existing").getBytes());
    		out.write(("<a href = 'delete'><h3>Click Here</a></h3></th>").getBytes());
    		out.write(("</table></body></html>").getBytes());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteMessage(BufferedOutputStream out) {

		File audioFile = new File(AUDIO_FOLDER + AUDIO_FILE);
		File textFile = new File(AUDIO_FOLDER + TEXT_FILE);
		int flag = 0;
			
		try {

			if(audioFile.exists()){
				audioFile.delete();
				Log.print("Successfully deleted the file: " + AUDIO_FOLDER + AUDIO_FILE);
			}
			else{
				Log.print("Error: No such file or directory" + AUDIO_FOLDER + AUDIO_FILE);
				flag ++;
			}

			if(textFile.exists()){
				textFile.delete();
				Log.print("Successfully deleted the file: " + AUDIO_FOLDER + TEXT_FILE);
			}
			else{
				Log.print("Error: No such file or directory" + AUDIO_FOLDER + TEXT_FILE);
				flag ++;
			}
			out.write(("<html><head><title>Delete</title></head><body>").getBytes());
			if(flag == 0)
			{
				out.write(("<h1 align='center'>Successfully deleted the existing message !!!").getBytes());
				out.write(("<h2 align='center'>Note: System will be using the default message</h2>").getBytes());
				out.write(("<h3 align='center'>To save a new message<h3>").getBytes());
				out.write(("<h3 align='center'><a href='index'>Click Here</a></h3>").getBytes());
				out.write(("</body></html>").getBytes());
			}
			else{
				out.write(("<h1 align='center'>Error: Cannot delete the default message").getBytes());
				out.write(("<br/><h3 align='center'>To save a new message</h3>").getBytes());
				out.write(("<h3 align='center'><a href='index'>Click Here</a></h3>").getBytes());
				out.write(("</body></html>").getBytes());
			}

		} catch (Exception x) {
			System.err.println(x);
		}
	}
	/**
	 * Parse the POST data from the request
	 */ 
	public String parsePOSTData(BufferedReader in){
		String data = "";
		try {
			String line = "";
	        // looks for post data
	        int postDataI = -1;
	        while ((line = in.readLine()) != null && (line.length() != 0)) {
	            //Log.print("HTTP-HEADER: " + line);
	            if (line.indexOf("Content-Length:") > -1) {
	                postDataI = new Integer(line.substring(line.indexOf("Content-Length:") + 16, line.length())).intValue();
	            }
	        }

	        String postDataStr = "";
	        // read the post data
	        if (postDataI > 0) {
	            char[] charArray = new char[postDataI];
	            in.read(charArray, 0, postDataI);
	            postDataStr = new String(charArray);
	            //Log.print(postDataStr);

	            String[] fields = postDataStr.split("=");
	            data = URLDecoder.decode(fields[1], "ISO-8859-15");
	        }
	    } catch (Exception e) {
			e.printStackTrace();
		}

	    return data;
	}


	public void returnHTMLFile(String fileName, int statusCode, 
								BufferedOutputStream out) {
		try{
			// write header
			String resHeader = createHeader(statusCode, 5);
			out.write(resHeader.getBytes());

			// write the html file
			parseHTMLFile(fileName, out);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*!
	 * Parse a HTML file
	 */
	public void parseHTMLFile(String fileName, BufferedOutputStream out) {
		try{
			FileInputStream fin = null; 

			try { 
				fin = new FileInputStream(fileName); 
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}

			int temp = 0; 
			byte[] buffer = new byte[1024]; 
			int bytes = 0; 
			while ((bytes = fin.read(buffer)) != -1 ) { 
				out.write(buffer, 0, bytes); 
				for(int iCount = 0; iCount < bytes; iCount++) { 
					temp = buffer[iCount]; 
				} 
			} 
			fin.close(); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


		/***
	 * Make the HTTP header for the response
	 */
	public String createHeader(int return_code, int file_type) {
	    String s = "HTTP/1.0 ";
	    //you probably have seen these if you have been surfing the web a while
	    switch (return_code) {
	      case 200:
	        s = s + "200 OK";
	        break;
	      case 400:
	        s = s + "400 Bad Request";
	        break;
	      case 403:
	        s = s + "403 Forbidden";
	        break;
	      case 404:
	        s = s + "404 Not Found";
	        break;
	      case 500:
	        s = s + "500 Internal Server Error";
	        break;
	      case 501:
	        s = s + "501 Not Implemented";
	        break;
	    }

	    s = s + "\r\n"; //other header fields,
	    s = s + "Connection: close\r\n"; //we can't handle persistent connections
	    s = s + "Server: Simple HTTP Server \r\n"; //server name

	    //Construct the right Content-Type for the header.
	    switch (file_type) {
	      //plenty of types for you to fill in
	      case 0:
	        break;
	      case 1:
	        s = s + "Content-Type: image/jpeg\r\n";
	        break;
	      case 2:
	        s = s + "Content-Type: image/gif\r\n";
	      case 3:
	        s = s + "Content-Type: application/x-zip-compressed\r\n";
	      default:
	        s = s + "Content-Type: text/html\r\n";
	        break;
	    }

	    s = s + "\r\n"; //this marks the end of the httpheader

	    return s;
    }
}



class HttpServer { 
	public static void main(String args[]) throws Exception { 

		// Check for default message;
		// create a HTTP server
		ServerSocket serverSocket = new ServerSocket(80); 
		// create a thread pool to handle client connections
		//ExecutorService threadPool = Executors.newFixedThreadPool(10);

		Log.print("Starting HTTP server...");

		while(true) { 
			Socket inSocket = serverSocket.accept();
			HttpRequest request = new HttpRequest(inSocket);
			request.run();
			request.check();
			// processing the request
			//threadPool.execute(request);
		} 
	} 
} 



