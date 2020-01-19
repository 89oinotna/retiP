


import exceptions.UserAlreadyExists;
import exceptions.UserNotExists;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;

public class Server {
    /**
     * RMI port 8082
     * UDP port 8081
     * TCP port 8080
     */
    public Users u; //struttura dati principale del server
    private ServerRMI serverrmi;
    private ServerTCP servertcp;
    private List<String> dict;
    private static final int n=200;

    public Server() {
        dict= Collections.synchronizedList(new ArrayList<String>(n));
        try {
            File file = new File("dict.txt");
            Scanner ss = new Scanner(file);
            while (ss.hasNextLine()) {
                String data = ss.nextLine();
                if (data.length() > 3) dict.add(data);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        long startTime = System.nanoTime();
        try (FileInputStream in = new FileInputStream("utenti.json");
             ) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();

            byte[] byteArray = new byte[1024]; // byte-array
            int bytesCount;
            while ((bytesCount = in.read(byteArray)) != -1) {
                out.write(byteArray, 0, bytesCount);}
            long elapsedTime = System.nanoTime() - startTime;
            System.out.println("Elapsed Time is " + (elapsedTime / 1000000.0)
                    + " msec");
            JSONArray utentiJSON = (JSONArray) (new JSONParser().parse(out.toString()));
            u = new Users(utentiJSON, dict);
        } catch (IOException | ParseException ex) { ex.printStackTrace(); }
        //try {

                //File file = new File("utenti.json");

               /* Scanner ss = new Scanner(file);
                StringBuilder json=new StringBuilder();


            while (ss.hasNext()) {
                    json.append(ss.next());
                }*/


            //StringBuilder json=new StringBuilder();
                //JSONArray utentiJSON = (JSONArray) (new JSONParser().parse(json.toString()));
                //u = new Users(utentiJSON, dict);
            //} catch (FileNotFoundException ex) {
            /*ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        serverrmi = new ServerRMI(8082, u);
        servertcp=new ServerTCP(8080, u);
        Thread tcpTH=new Thread(servertcp);
        tcpTH.start();
    }

    public void save() {
        try {

            FileChannel outChannel = FileChannel.open(Paths.get("utenti.json"), StandardOpenOption.WRITE);


            System.out.println("Writing JSON object to file");
            JSONArray utentiJSON = new JSONArray();
            for (Iterator<User> it = u.getIterator(); it.hasNext(); ) {
                utentiJSON.add(it.next().toJSON());
            }


            ByteBuffer buffer = ByteBuffer.wrap(utentiJSON.toJSONString().getBytes());
            outChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server s = new Server();

        //s.test1();
        Scanner sc=new Scanner(System.in);
        if(sc.nextInt()>0){
            s.save();
        }
        //s.save();
    }



}
