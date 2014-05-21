package sip;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SDPPacket {
	String username;
	String sessionId;
	String sessionVersion;
	int rtpPort;

	public SDPPacket() {
		
	}

	public static SDPPacket parseFromRequest(String request) { 
		SDPPacket packet = new SDPPacket();
		try {

			// extract username, sessionId and sessionVersion
			Pattern pattern = Pattern.compile("o=(\\S+)\\s(\\S+)\\s(\\S+)");
			Matcher matcher = pattern.matcher(request);
			matcher.find();
			packet.username = matcher.group(1);
			packet.sessionId = matcher.group(2);
			packet.sessionVersion = matcher.group(3);

			// extract rtp port
			pattern = Pattern.compile("m=\\S+\\s(\\d+)");
			matcher = pattern.matcher(request);
			matcher.find();
			packet.rtpPort = Integer.parseInt(matcher.group(1));

			return packet;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	} 

	public String toString() {
		return "Username: " + username + "\n"
				+ "SessionId: " + sessionId + "\n"
				+ "Session version: " + sessionVersion + "\n"
				+ "RTP port: " + rtpPort + "\n";

	}
}