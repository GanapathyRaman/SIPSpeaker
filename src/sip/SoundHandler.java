package sip;

import java.util.*;
import java.net.*; 
import java.io.*; 
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

public class SoundHandler {
	private static final String AUDIO_FOLDER = "audio/";
	private static final String TEMPORARY_AUDIO_FOLDER = "audio/tmp/";

	private String TEXT_FILE = "currentmessage.txt";
	private String AUDIO_FILE = "currentmessage.wav";
	private String DEFAULT_MSG = "Welcome to SIP Speaker. This is my own answering machine. You have no new messages.";
	private String DEFAULT_TEXT_FILE = "default.txt";
	private String DEFAULT_AUDIO_FILE = "default.wav";

	public SoundHandler(String defaultFilename, 
						String defaultMessage, 
						String currentFilename) {
		
		if (defaultFilename != "" ){
			this.DEFAULT_AUDIO_FILE =  defaultFilename;
			this.DEFAULT_TEXT_FILE = defaultFilename.substring(0, defaultFilename.toLowerCase().indexOf(".wav"));
		}

		if (defaultMessage != ""){
			this.DEFAULT_MSG = defaultMessage;
		}

		if (currentFilename != "" ){
			this.AUDIO_FILE =  currentFilename;
			this.TEXT_FILE = currentFilename.substring(0, currentFilename.toLowerCase().indexOf(".wav"));
		}

		dataInAudioFormat(DEFAULT_MSG, DEFAULT_AUDIO_FILE, DEFAULT_TEXT_FILE);
	 	Log.print("Default File has be successfully created");
	}

	/*!
	 * Create new message
	 */
	public synchronized void createNewMessage(String message) {
		dataInAudioFormat(message, AUDIO_FILE, TEXT_FILE);
	}

	/*!
	 * Delete the current message
	 */
	public synchronized boolean deleteCurrentMessage() {
		File audioFile = new File(AUDIO_FOLDER + AUDIO_FILE);
		File textFile = new File(AUDIO_FOLDER + TEXT_FILE);
		boolean success = true;
			
		try {
			if(audioFile.exists()){
				audioFile.delete();
				Log.print("Successfully deleted the file: " + AUDIO_FOLDER + AUDIO_FILE);
			}
			else{
				Log.print("Error: No such file or directory" + AUDIO_FOLDER + AUDIO_FILE);
				success = false;
			}

			if(textFile.exists()){
				textFile.delete();
				Log.print("Successfully deleted the file: " + AUDIO_FOLDER + TEXT_FILE);
			}
			else{
				Log.print("Error: No such file or directory" + AUDIO_FOLDER + TEXT_FILE);
				success = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return success;
	}

	/*!
	 * Get the current message
	 */
	public synchronized String getCurrentMessage() {
		String message = "";
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

    	return message;
	}

	/*!
	 * Get the current sound filename. If it is not available, return the default file.
	 */
	public String getCurrentSoundFile() {
		try {
			String filename = AUDIO_FOLDER + AUDIO_FILE;
			File audioFile = new File(filename);
			if (audioFile.exists()) {
				return filename;
			}

			filename = AUDIO_FOLDER + DEFAULT_AUDIO_FILE;
			audioFile = new File(filename);
			if (audioFile.exists()) {
				return filename;
			}
		} catch (Exception e) {
			e.printStackTrace();

		}

		return null; 
	}

	public String generateTemporaryFilename(String id) {
		return TEMPORARY_AUDIO_FOLDER + id + ".wav";
	}

	/*
	* Writing message in .WAV format
	*/
	private void dataInAudioFormat(String message, String audiofile, String textfile)
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

        	VoiceManager vm = VoiceManager.getInstance();
        	Voice voice = vm.getVoice("kevin16");
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
}