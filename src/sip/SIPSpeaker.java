package sip;

import java.net.*; 
import java.io.*; 
import java.util.*; 

public class SIPSpeaker {
	private SIPServer mSipServer = null;
	private HttpServer mHttpServer = null;
	private SoundHandler mSoundHandler = null;
	
	public SIPSpeaker(Properties props) throws Exception{
		mSoundHandler = new SoundHandler(props.getProperty(SettingsHandler.DEFAULT_WAV),
											props.getProperty(SettingsHandler.DEFAULT_TEXT),
											props.getProperty(SettingsHandler.CURRENT_WAV));
		//mSoundHandler = new SoundHandler("default.wav", "Hello what the fuck are you doing", "currentmessage.wav");

		try {
			mSipServer = new SIPServer(props.getProperty(SettingsHandler.SIP_USER),
										props.getProperty(SettingsHandler.SIP_INTERFACE),
										Integer.parseInt(props.getProperty(SettingsHandler.SIP_PORT)),
										mSoundHandler);
			//mSipServer = new SIPServer("sipspeaker", "127.0.0.1", 8888, mSoundHandler);
		} catch (Exception e) {
			throw new Exception("Unable to initialize the SIP server: " + e.getMessage());
		}

		try {
			mHttpServer = new HttpServer(props.getProperty(SettingsHandler.HTTP_INTERFACE),
										Integer.parseInt(props.getProperty(SettingsHandler.HTTP_PORT)),
										mSoundHandler);
			//mHttpServer = new HttpServer("0.0.0.0", 8080, mSoundHandler);
		} catch (Exception e) {
			if (mSipServer != null) {
				mSipServer.interrupt();	
			}
			throw new Exception("Unable to initialize the HTTP server: " + e.getMessage());
		}
	}

	public void run() {
		mSipServer.start();
		mHttpServer.start();
	}

	public void stop() {
		try {
			if (mSipServer != null) {
				mSipServer.interrupt();	
			}
			if (mHttpServer != null) {
				mHttpServer.interrupt();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
	
		try {
			System.setProperty("java.awt.headless", "false");
			SettingsHandler settingsHandler = new SettingsHandler();
			Properties props = settingsHandler.parse(args);	
			SIPSpeaker sipSpeaker = new SIPSpeaker(props);
			sipSpeaker.run();
		} catch (Exception e) {
			System.err.println(e.getMessage());

			return;
		}
	}
}
