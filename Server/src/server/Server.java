package server;


import Settings.Settings;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    private UDP serverudp;

    public Server() {

        //Leggi il dizionario e settalo per le sfide
        dict= new ArrayList<>();
        new File("dict.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader("dict.txt"))) {
            String parola;
            while ((parola=reader.readLine())!=null) {
                 dict.add(parola);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        Challenge.setDict(dict);

        //Leggi il file degli utenti, se non esiste crealo, e crea l'oggetto
        try (FileInputStream in = new FileInputStream("utenti.json")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] byteArray = new byte[1024]; // byte-array
            int bytesCount;
            while ((bytesCount = in.read(byteArray)) != -1) {
                out.write(byteArray, 0, bytesCount);
            }
            JSONArray utentiJSON = (JSONArray) (new JSONParser().parse(out.toString()));
            u = new Users(utentiJSON);

        } catch (IOException | ParseException ex) {
            u=new Users();
        }

        serverrmi = new ServerRMI(Settings.RMIPort, u);
        serverudp=new UDP(Settings.UDPPort);
        servertcp=new ServerTCP(Settings.TCPPort, u, serverudp);
        Thread tcpTH=new Thread(servertcp);
        tcpTH.start();
    }

    /**
     * Esegue il salvataggio del server nel file utenti.json
     */
    public void save() {
        try {

            FileChannel outChannel = FileChannel.open(Paths.get("utenti.json"), StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            System.out.println("Writing JSON object to file");
            JSONArray utentiJSON = new JSONArray();
            for (Iterator<User> it = u.getIterator(); it.hasNext(); ) {
                utentiJSON.add(it.next().toJSON());
            }
            ByteBuffer buffer = ByteBuffer.wrap(utentiJSON.toJSONString().getBytes()); //Brutale?
            outChannel.write(buffer);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //todo thread che salva periodicamente
        Server s = new Server();
        /*
        Blocco il server in readline
        Se si inserisce un numero>0 il server salva
            altrimenti salva e si chiude
         */
        Scanner sc=new Scanner(System.in);
        while(sc.nextInt()>0){
            s.save();
        }
        s.save();

        System.exit(0);
    }



}
