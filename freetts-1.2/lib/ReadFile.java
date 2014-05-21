import java.io.*;

public class ReadFile {

    public static void main(String[] args){
        String string="";
        String file ="audio/omg.txt";

        //reading   
        try{
            InputStream ips=new FileInputStream(file); 
            InputStreamReader ipsr=new InputStreamReader(ips);
            BufferedReader br=new BufferedReader(ipsr);
            String line;
            while ((line=br.readLine())!=null){
                System.out.println(line);
                string+=line+"\n";
            }
            br.close(); 
        }       
        catch (Exception e){
            System.out.println(e.toString());
        }
}
}
