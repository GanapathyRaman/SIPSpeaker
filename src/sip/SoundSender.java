package sip;

import java.io.*;
import javax.media.*;
import java.net.*;
import javax.media.rtp.*;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

public class SoundSender  {

	private String destAddr;
	private int destRTPPort;
	private String localAddr;
	private SoundHandler soundHandler;

	private Processor mediaProcessor;
	private RTPManager rtpManager;
	private SendStream sendStream;
	private String temporaryFilename;

	private boolean stopped;


	/*private int localRtpPort;
	private static int BASE_RTP_PORT = 16000;
	private static int INCREMENT = 0;*/
	
	public SoundSender(String destAddr, int destRTPPort, String localAddr, 
								String id, SoundHandler soundHandler)  {
		this.destAddr = destAddr;
		this.destRTPPort = destRTPPort;
		this.localAddr = localAddr;
		this.soundHandler = soundHandler;
		
		temporaryFilename = soundHandler.generateTemporaryFilename(id);
		stopped = false;

		createTemporaryMediaFile();

		/*INCREMENT += 2;
		localRtpPort = BASE_RTP_PORT + INCREMENT;
		if (localRtpPort > 60000) {
			INCREMENT = 0;
		}*/
	}

	public void start() {
		try {
			rtpManager = RTPManager.newInstance();

			// initialize the RTPManager
			Log.print(localAddr);
			SessionAddress localSessionAddr = new SessionAddress(InetAddress.getByName(localAddr), SessionAddress.ANY_PORT);
			rtpManager.initialize(localSessionAddr);

			// open the connection
			InetAddress destInetAddr = InetAddress.getByName(destAddr);
			SessionAddress remoteSessionAddr = new SessionAddress(destInetAddr, destRTPPort);
			rtpManager.addTarget(remoteSessionAddr);

 			File mediaFile = new File(temporaryFilename);
	        DataSource source = Manager.createDataSource(new MediaLocator(mediaFile.toURL()));

 			
	        Format[] FORMATS = new Format[]{new AudioFormat(AudioFormat.GSM_RTP, 8000, 8, 1)};
	        ContentDescriptor CONTENT_DESCRIPTOR = new ContentDescriptor(ContentDescriptor.RAW_RTP);
	        mediaProcessor = Manager.createRealizedProcessor(new ProcessorModel(source, FORMATS, CONTENT_DESCRIPTOR));

	        // start the processor
	        mediaProcessor.start();

	        sendStream = rtpManager.createSendStream(mediaProcessor.getDataOutput(), 0);
        	sendStream.start();

	        double duration = mediaProcessor.getDuration().getSeconds();
	        System.out.println("Duration of wav file: " + duration + "\r\n");
	        
	        //wait until the file is transmitted
	        Thread.sleep(1000 + 1000 * (int) duration);

	        //close();

	        
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*!
	 * Close the connection if no longer needed.
	 */
	public void close() {
		Log.print("Closing sending stream");
		if (stopped) {
			return;
		}
		stopped = true;
		try {
			// stop send stream
			sendStream.stop();
			sendStream.close();

			// stop media processor
			mediaProcessor.stop();
	        mediaProcessor.close();

	        // stop rtp session
			rtpManager.removeTargets("Session stopped.");
			rtpManager.dispose();

			// delete media file
			File file = new File(temporaryFilename);
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	private synchronized void createTemporaryMediaFile() {
		InputStream input = null;
		OutputStream output = null;

		try {
			File source = new File(soundHandler.getCurrentSoundFile());
			File dest = new File(temporaryFilename);

			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
				output.close();
			} catch (Exception e) {}
			
		}
	}

	private void sleep(long milliSeconds) {
		try { 
			Thread.sleep(milliSeconds);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
