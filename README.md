#  Project -  SIP SPeaker
#  Preferred platform for running the server - Any Linux Machine
#  Dependencies - 
#           * For the Server - All the voice Jar Files form Freetts-1.2 and jmf.jar (Java Media for RTP
#           * For the Client - Any SIP softphone (PC-PC), preferably Linphone or SJPhone
#
#  +----------+----------------------+-------------------------------------------+
#  | Authors |  Tien Thanh Bui  | Ganapathy Raman Madanagopal  |
#  +----------+----------------------+-------------------------------------------+
#  |  Email  |    <ttbu@kth.se>   |               <grma@kth.se>               |
#  +---------+-----------------------+-------------------------------------------+
#
#  The following step can be use to run the Webmail Server
#  1. The src folder consist of 
#          * An Unix shell script - runsip
#	* SIP Configuration file - sipspeaker.cfg
#          * Folder HTML
#              * Contains five HTML files - 404.htm  index.htm  result.htm  empty.htm  voice.htm
#          * Folder Audio
#              * Contains default.wav and default.txt and a temporary folder.
#          * Folder Libs
#              * Contains ten jar files of Freetts, one jmf.jar (for RTP) and org.xbill.dns_2.1.6.jar for  
        DNS functionalities.
#          * Folder SIP
#              * Contains eleven java files for SIP, SDP, RTP, Web, DNS, Server Log functionality
#               Files:
#	              DNSHandler.java  RTPSocketAdapter.java  SIPPacket.java  SoundHandler.java
#                        HttpServer.java  SDPPacket.java  SIPServer.java  SoundSender.java
#                        Log.java  SettingsHandler.java  SIPSpeaker.java
#  2. Download the entire package under the folder “src”.
#  3. If required change the permission of the directory (i.e. chmod -R 777 src).
#  5. To change the SIP configuration edit the required parametes in the file “sipspeaker.cfg”
#  5. Now run the shell script “runsip” using the command “sh runsip” or “./runsip”
#  6. Also to override the parameters specified in the configuration file, you can specify the 
#      required parameters under CLI when while you start the server.
#      Eg: ./runsip [-user <username>@<ip/domain>:<port>] [-c new_configuration_file_name] 
#                         [-http http_bind_address]
#      Note: The parameters that are not specified in CLI will taken be from the configuration file.
#  7. Now the script will compile the java files and start will the SIP Speaker and the Web Server 
#      automatically on the specified IP and port
#  8. At this point you should be able to see
#      The Parameters that SIP Server and Web Server uses.
#      “The SIP Server is running on <ip>:<sip_port> …”
#      “Starting HTTP serv er on <ip>:<http_port> …”
#  9. To access Web Portal, open a web browser and type 
#        “http://<ip-address-of-the-server>:<http-bind-port>”
#  10. For the clients UA, open any SIP Softphones, preferably Linphone or SJPhone and call to 
#         the SIP Speaker/Server using the foloowing URI syntax
#         “sip:<server_username>@<ip/domain>:<port>”
