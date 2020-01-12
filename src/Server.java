


import exceptions.UserAlreadyExists;
import exceptions.UserNotExists;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.*;

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
    public Utenti u; //struttura dati principale del server
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
        try {
            /*
            if (new File("utenti.json").createNewFile()) {
                u = new Utenti(dict);
            } else {
                FileChannel inChannel = FileChannel.open(Paths.get("utenti.json"), StandardOpenOption.READ);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                String json = "";
                byte[] b;//=new byte[1024];
                while (inChannel.read(buffer) > 0) {
                    buffer.flip();
                    b = new byte[buffer.remaining()];
                    buffer.duplicate().get(b);
                    //buffer.get(buffer.limit());
                    json += new String(b);
                    buffer.clear();

                }*/
                //System.out.println(json);
                File file = new File("utenti.json");
                Scanner ss = new Scanner(file);
                StringBuilder json=new StringBuilder();
                while (ss.hasNext()) {
                    json.append(ss.next());
                }
                JSONArray utentiJSON = (JSONArray) (new JSONParser().parse(json.toString()));
                u = new Utenti(utentiJSON, dict);
            } catch (FileNotFoundException | ParseException ex) {
            ex.printStackTrace();
        }


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
            for (Iterator<Utente> it = u.getIterator(); it.hasNext(); ) {
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

    public void test1() {
        int i = 0;
        /**
         * test per registra utente
         */
        this.u.registraUtente("antonio", "asd");    //nuovo utente
        try {
            this.u.registraUtente("antonio", "asd");
        }   //utente già registrato
        catch (UserAlreadyExists e) {
            i++;
            assert (i == 1);
        }
        this.u.registraUtente("alfredo", "asd1");   //nuovo utente


        /**
         * test per login utente
         */
        //todo se è gia loggato e si logga con pw sbagliata dico che è già online o che la password è sbagliata?
        //assert (!this.u.loginUtente("antonio", "asd1")); //password errata
        //assert (this.u.loginUtente("antonio", "asd")); //tutto corretto
        //assert (!this.u.loginUtente("antonio", "asd"));   //utente loggato
        //assert (!this.u.loginUtente("giovanni", "asd")); //utente che non esiste

        /**
         * test per logout utente
         */
        this.u.logoutUtente("antonio");
        this.u.logoutUtente("antonio");
        //assert (this.u.loginUtente("antonio", "asd")); //tutto corretto

        /**
         * test aggiunta amico
         */
        try {
            this.u.aggiungiAmicizia("antonio", "pasquale");
        }   //amico non esiste
        catch (UserNotExists e) {
            i++;
            assert (i == 2);
        }

        this.u.aggiungiAmicizia("antonio", "alfredo"); //amico esistente

        /**
         * test punteggio
         */
        this.u.aggiornaPunteggio("antonio", 10);
        assert (this.u.mostraPunteggio("antonio") == 10);
    }

}
