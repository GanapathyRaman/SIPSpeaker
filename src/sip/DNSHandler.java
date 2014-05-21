package sip;

import java.util.*;
import java.io.*;
import java.net.*;
import org.xbill.DNS.*;

public class DNSHandler {
    /*!
     * Look up the mail server of the given email address
     */
    public static String getSmtpServer(String email) {
        try {
            String domain = email.substring(email.indexOf("@")+1);
            Log.print("Looking up SMTP server for domain: " + domain);
            Record[] records = new Lookup(domain, Type.MX).run();

            int minPriority = Integer.MAX_VALUE;            
            String mailServer = "";
            for (int i = 0; i < records.length; i++) {
                MXRecord mx = (MXRecord) records[i];
                if (mx.getPriority() < minPriority) {
                    mailServer = mx.getTarget().toString();
                    minPriority = mx.getPriority();
                }
            }
            Log.print("SMTP server of: " + domain + " is: " + mailServer);
            return mailServer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /*!
     * Resolve a domain name
     */
    public static InetAddress resolveDomain(String domainName) {
        try {
            Lookup lookup = new Lookup(domainName, Type.A, DClass.IN);  
            Record[] records = lookup.run();  
            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                ARecord record = (ARecord) records[0];
                return record.getAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}