package sip;

import java.net.*; 
import java.io.*; 
import java.util.*; 
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SIPServer extends Thread{
	private String mServerInterface;
	private String mServerTag;
	private String mUsername;
	private int mPort;
	private DatagramSocket mServerSocket;
	private Map<String, Session> mSessionMap;
	private SoundHandler mSoundHandler;

	public SIPServer(String username, String serverInterface, int port, 
						SoundHandler soundHandler) throws Exception {
		mUsername = username;
		mServerInterface = serverInterface;
		mPort = port;
		mServerTag = "81374918749823742387";
		mSoundHandler = soundHandler;

		mSessionMap = new HashMap<String, Session>();

		// create a SIP server
		mServerSocket = new DatagramSocket(mPort, InetAddress.getByName(mServerInterface)); 
		Log.print("The SIP Server is running on " + mServerInterface + ":" + mPort + " ...");
	}

	@Override
	public void run() {
		/*String s = "CANCEL sip:localhost:8888 SIP/2.0\nVia: SIP/2.0/UDP 127.0.0.1;rport;branch=z9hG4bKc0a8016700000017534e4d7b15c8ed1b00000001\nContent-Length: 0\nCall-ID: 9D2CEFC0-1DD1-11B2-83F2-D303615B192A@192.168.1.103\nCSeq: 1 CANCEL\nFrom: \"Thanh\"<sip:127.0.0.1>;tag=17761999501525676221\nMax-Forwards: 70\nTo: <sip:localhost:8888>\nUser-Agent: SJphone/1.60.299a/L (SJ Labs)\n";
		SIPPacket packet = SIPPacket.parseFromRequest(s);
		Log.print(packet.toString());*/

		try {
			byte[] receiveData = new byte[1024];             

			// create a thread pool to handle client connections
			ExecutorService threadPool = Executors.newFixedThreadPool(20);

			while (!Thread.currentThread().isInterrupted()) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				mServerSocket.receive(receivePacket);

				SIPPacketHandler handlerThread = new SIPPacketHandler(receivePacket);
				threadPool.execute(handlerThread);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Session getSession(String id) {
		return mSessionMap.get(id);
	}

	private void saveSession(String id, Session session) {
		mSessionMap.put(id, session);
	}

	private void removeSession(String id) {
		mSessionMap.remove(id);
	}

	class Session {
		static final int ESTABLISHING = 1;
		static final int ESTABLISHED = 2;
		static final int TEARING_DOWN = 3;
		static final int FINISHED = 4;

		String id;
		int status;
		SDPPacket sdpPacket = null;
		SoundSender soundSender = null;

		public Session(String id) {
			this.id = id;
			status = ESTABLISHING;
		}

		public void tearDown() {
			status = FINISHED;
			if (soundSender != null) {
				soundSender.close();
			}
		}
	}

	class SIPPacketHandler implements Runnable {
		DatagramPacket dataPacket;
		SIPPacket sipPacket = null;
		InetAddress srcAddress = null;
		int srcPort;

		public SIPPacketHandler(DatagramPacket receivePacket) {
			dataPacket = receivePacket;
		}

		public void run() {
			if (dataPacket == null) {
				return;
			}

			try {
				String dataStr = new String(dataPacket.getData());
				//Log.print(dataStr);

				sipPacket = SIPPacket.parseFromRequest(dataStr);
				srcAddress = dataPacket.getAddress();
				srcPort = dataPacket.getPort();
				

				Log.print(sipPacket.toString());
				//Log.print(sipPacket.type);

				// new session 
				if (sipPacket.type.equals("OPTIONS")) {
					handleOptionsRequest(dataStr);
				} else if (sipPacket.type.equals("INVITE")) {
					handleInviteRequest(dataStr);
				} else if (sipPacket.type.equals("ACK")) {
					handleAckRequest(dataStr);
				} else if (sipPacket.type.equals("BYE")) {
					handleByeRequest(dataStr);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void handleOptionsRequest(String dataStr) throws Exception{
			sendOkMessage(null);
		}

		private void handleInviteRequest(String dataStr) throws Exception{
			// check if username matches
			if (!sipPacket.destUsername.equals(mUsername)) {
				send404NotFoundMessage();
				return;
			}
			Session session = new Session(sipPacket.callId);
			session.sdpPacket = SDPPacket.parseFromRequest(dataStr);
			//Log.print(session.sdpPacket.toString());

			saveSession(sipPacket.callId, session);
			sendTryingMessage();
			sendRingingMessage();
			sendOkMessage(session.sdpPacket);
		}

		private void handleAckRequest(String dataStr) throws Exception{
			Session session = getSession(sipPacket.callId);
			if (session == null) {
				// TODO: return something meaningful
				return;
			}
			session.status = Session.ESTABLISHED;
			session.soundSender = new SoundSender(sipPacket.srcAddr, 
														session.sdpPacket.rtpPort,
														sipPacket.destAddr,
														session.id,
														mSoundHandler);
			session.soundSender.start();
			if (session.status != Session.FINISHED) {
				session.tearDown();
				removeSession(session.id);
				sendByeMessage();	
			}
		}

		private void handleByeRequest(String dataStr) throws Exception{
			Session session = getSession(sipPacket.callId);
			if (session == null) {
				// TODO: return something meaningful
				return;
			}
			if (session.status != Session.FINISHED) {
				session.tearDown();
				removeSession(session.id);
				sendOkMessage(null);
			}
		}

		private void sendTryingMessage() {
			String message = createSIPHeader("100 Trying", 0, null);
			sendMessage(message);
		}

		private void sendRingingMessage() {
			String message = createSIPHeader("180 Ringing", 0, null);
			sendMessage(message);
		}

		private void sendOkMessage(SDPPacket sdpPacket) {
			String message = null;
			
			if (sdpPacket != null) {
				String sdpMessage = createSDPMessage(sdpPacket);
				message = createSIPHeader("200 OK", sdpMessage.length(), "application/sdp");
				message += sdpMessage;
			} else {
				message = createSIPHeader("200 OK", 0, null);	
			}
			sendMessage(message);	
		}

		private void send404NotFoundMessage() {
			String message = createSIPHeader("404 Not Found", 0, null);
			sendMessage(message);
		}

		private void sendByeMessage() {
			String message = "BYE sip:" + sipPacket.srcAddr + " SIP/2.0\r\n"
                + "Via: SIP/2.0/UDP " + sipPacket.destAddr + ";"
                + "rport;"
                + "branch=" + sipPacket.branch + "\r\n"
                + "Content-Length: 0\r\n"	
                + "Call-ID: " + sipPacket.callId + "\r\n"
                + "CSeq: 2 BYE\r\n"
                + "From: \"" + mUsername + "\"<sip:" + mUsername + "@" + sipPacket.destAddr + ">;"
                + "tag=" + mServerTag + "\r\n"
                + "Max-Forwards: 70\r\n"
                + "To: <sip:" + sipPacket.srcAddr + ">;"
                + "tag=" + sipPacket.srcTag + "\r\n\r\n";
        	sendMessage(message);
		}

		/*!
		 * Create a header for SIP status message with the given type and contentLength
		 */
		private String createSIPHeader(String packetType, int contentLength, String contentType) {
			String s =  "SIP/2.0 " + packetType + "\r\n"
                + "Via: SIP/2.0/UDP " + sipPacket.srcAddr + ";"
                + "rport=" + mPort + ";"
                + "received=" + sipPacket.srcAddr + ";"
                + "branch=" + sipPacket.branch + "\r\n"
                + "Content-Length: " + contentLength + "\r\n"
                + "Contact: <sip:" + sipPacket.destAddr + ":" + mPort + ">\r\n"
                + "Call-ID: " + sipPacket.callId + "\r\n";
            if (contentType != null) {
            	s += "Content-Type: " + contentType + "\r\n";
            } 
            s += "CSeq: " + sipPacket.cseq + "\r\n"
                + "From: " + sipPacket.srcUsername + "<sip:" + sipPacket.srcAddr + ">;"
                + "tag=" + sipPacket.srcTag + "\r\n"
                + "To: \"" + mUsername + "\"<sip:" + mUsername + "@" + sipPacket.destAddr + ">;"
                + "tag=" + mServerTag + "\r\n\r\n";
			return s;
		}

		private String createSDPMessage(SDPPacket sdpPacket) {
			String s = "v=0" + "\r\n"
                + "o=" + sdpPacket.username + " "
                		+ sdpPacket.sessionId + " " 
                		+ sdpPacket.sessionVersion + " " 
                		+ "IN IP4 " + sipPacket.destAddr + "\r\n"
                + "s=Talk\r\n"
                + "c=IN IP4 " + sipPacket.destAddr + "\r\n"
                + "t=0 0\r\n"
                + "m=audio " + sdpPacket.rtpPort + " RTP/AVP 3 101\r\n"
                /*+ "a=rtpmap:111 speex/16000" + "\r\n"
				+ "a=fmtp:111 vbr=on" + "\r\n";*/
                + "a=sendrecv" + "\r\n"
                + "a=rtpmap:3 GSM/8000" + "\r\n"
				+ "a=rtpmap:101 telephone-event/8000" + "\r\n"
				+ "a=fmtp:101 0-16" + "\r\n";
            return s;
		}

		private void sendMessage(String message) {
			//Log.print(message);
			try {
				byte[] sendData = message.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, 
																sendData.length, 
																srcAddress, 
																srcPort);
	        	mServerSocket.send(sendPacket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
