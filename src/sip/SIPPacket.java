package sip;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SIPPacket {
	String type;
	String callId;
	String cseq;
	String srcAddr;
	String srcUsername;
	String srcTag;
	String destAddr;
	String destUsername;
	String branch;

	public SIPPacket() {
		
	}

	public static SIPPacket parseFromRequest(String request) { 
		SIPPacket packet = new SIPPacket();
		try {
			// extract packet type
			String firstLine = request.substring(0, request.indexOf("\n"));
			String[] tmp = firstLine.split(" ");
			if (tmp[0].equals("SIP/2.0")) {
				packet.type = tmp[2];
			} else {
				packet.type = tmp[0];
			}

			// extract call-id
			Pattern pattern = Pattern.compile("(Call-ID:\\s)(\\S+)");
			Matcher matcher = pattern.matcher(request);
			matcher.find();
			packet.callId = matcher.group(2);

			// extract cseq
			pattern = Pattern.compile("(CSeq:\\s)(\\S+\\s\\S+)");
			matcher = pattern.matcher(request);
			matcher.find();
			packet.cseq = matcher.group(2);

			// extract source address and username
			String[] patternStr = {"From:\\s(\\S+)\\s*<sip:([a-z0-9\\.-]+)>",
									"From:\\s<sip:(\\S+)@([a-z0-9\\.-]+)[:>]",
									"From:\\s<sip:([a-z0-9\\.-]+)[:>]"};
			for (int i = 0; i < patternStr.length; i++) {
				pattern = Pattern.compile(patternStr[i]);
				matcher = pattern.matcher(request);	
				if (matcher.find()) {
					if (matcher.groupCount() == 2) {
						packet.srcUsername = matcher.group(1);
						packet.srcAddr = matcher.group(2);			
					} else {
						packet.srcUsername = "";
						packet.srcAddr = matcher.group(1);	
					}
					break;
				}
			}

			// extract source tag
			pattern = Pattern.compile("From:\\s.+tag=(\\w+)");
			matcher = pattern.matcher(request);
			matcher.find();
			packet.srcTag = matcher.group(1);

			// extract destination address and username
			patternStr = new String[]{"To:.+<sip:(\\S+)@([a-z0-9\\.-]+)[:>]",
							"To:.+<sip:([a-z0-9\\.-]+)[:>]"};
			for (int i = 0; i < patternStr.length; i++) {
				pattern = Pattern.compile(patternStr[i]);
				matcher = pattern.matcher(request);	
				if (matcher.find()) {
					if (matcher.groupCount() == 2) {
						packet.destUsername = matcher.group(1);
						packet.destAddr = matcher.group(2);	
					} else {
						packet.destUsername = "";
						packet.destAddr = matcher.group(1);
					}
					break;
				}
			}

			// extract branch
			pattern = Pattern.compile("(branch=)(\\w+)");
			matcher = pattern.matcher(request);
			matcher.find();
			packet.branch = matcher.group(2);
			
			return packet;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	} 

	public String toString() {
		return "Type: " + type + "\n"
				+ "Call-ID: " + callId + "\n"
				+ "Cseq: " + cseq + "\n"
				+ "From: " + srcUsername + " " + srcAddr + "; " 
				+ "Tag: " + srcTag + "\n"
				+ "To: " + destUsername + " " + destAddr + "\n"
				+ "Branch: " + branch + "\n";

	}
}