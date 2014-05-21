package sip;

import java.util.*; 
import java.text.SimpleDateFormat;
import java.text.DateFormat;

public class Log {

	private static boolean enableLog = true;	//Enable or Disable server logs

	public static void print(String message) 
	{
		if (enableLog == true) {
			Calendar cal = Calendar.getInstance();
			Date date = cal.getTime(); 
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			String time = dateFormat.format(date);

			//Print the log along with the time
			System.out.println("\n[" + time + "] "+ message);
		}
	}
}