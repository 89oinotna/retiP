package client;

import Settings.Settings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.UDP;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

//LOGIN E REGISTRAZIONE WRITE E READ SERIALI NELLA STESSA FUNZIONE
//ALTRI COMANDI:
public class ClientTCP implements Runnable{
    private SocketAddress address;
    private SocketChannel socketChannel;
    private ByteBuffer byteBuffer;
    private int BUFFLEN=1024;
    private Scanner scanner;
    private List<String> pendingFriendsList;
    private List<String> friendsList;
    private List<String> classificaList;
    private String token;
    private ConcurrentHashMap<String, LocalDateTime> richiesteSfida;
    private StringBuilder lastResponse;
    private String loggedNick;
    private List<String> sfida;

    private UDP udp;

    public ClientTCP(List<String> sfida,
                     List<String> pendingFriendsList,
                     List<String> friendsList,
                     List<String> classificaList,
                     ConcurrentHashMap<String, LocalDateTime> richiesteSfida){
        this.sfida=sfida;
        this.pendingFriendsList=pendingFriendsList;
        this.friendsList=friendsList;
        this.classificaList=classificaList;
        this.richiesteSfida=richiesteSfida;
        address = new InetSocketAddress("127.0.0.1", 8080);
        lastResponse=new StringBuilder();

        try {
            socketChannel = SocketChannel.open();
            //socketChannel.configureBlocking(false);
            socketChannel.connect(address);
            //socketChannel.configureBlocking(false);

            byteBuffer=ByteBuffer.allocate(BUFFLEN);
            while (!socketChannel.finishConnect()) {
                System.out.println("Non terminata la connessione");
            }
            System.out.println("Terminata la connessione");


            scanner = new Scanner(new InputStreamReader(socketChannel.socket().getInputStream(), "ASCII"));
            //scanner.useDelimiter("\n");


        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    public String read(){
        String response=scanner.nextLine();
        while(response==null) {
            response = scanner.nextLine();
        }
        return response;
    }

    public void send(String message){
        //todo aggiusta
        System.out.println("REQUEST: "+message);
        ByteBuffer buf = ByteBuffer.wrap((message+"  \n").getBytes());
        try {
            while (buf.hasRemaining()) {
                int w=socketChannel.write(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        buf.clear();
    }

    /**
     * effettua il login su tCP
     * @param nick
     * @param pw
     * @return il token per la sessione
     */
    public String login(String nick, String pw, int udpPort){
        String message="LOGIN "+nick+" "+pw+" "+udpPort;
        send(message);
        String response=read();
        return response;
    }


    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){

            String response=read();
            System.out.println("RESPONSE: "+response);
            manageCommand(response);
        }
    }

    /**
     * restituisce l'ultima risposta a un comando
     * @return
     */
    public synchronized String getResponse(){

        synchronized (lastResponse){
            lastResponse.setLength(0);
            while(lastResponse.length()<1) {
                try {
                    lastResponse.wait();


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            String response=lastResponse.toString();
            lastResponse.setLength(0);
            return response;
        }


    }

    /**
     * Gestisce i comandi ricevuto
     * @param response
     */
    public void manageCommand(String response){
        //todo cambiare risposte server


        String[] tokens=response.split(" ");
        //quelli che iniziano con OK NOK sono in risposta a richieste
        if ((tokens[0].equals("OK") || tokens[0].equals("NOK")) && tokens[2].equals(token)) {
            synchronized (lastResponse) {
                lastResponse.append(response);
                lastResponse.notify();
            }


        }

        else if(tokens[1].equals(token)) { //validazione tramite token della risposta

            //ASYNC
            switch (tokens[0]) {
                //AMICIZIA TOKEN NICK TYPE
                case "AMICIZIA":
                    manageAmicizia(tokens[2], tokens[3]);
                    break;
                case "AMICI":
                    //todo oggettojson
                    manageAmici(tokens[2]);
                    break;
                case "CLASSIFICA": //async
                    //todo oggettojson classifica
                    manageClassifica(tokens[2]);
                    break;
                case "PENDING":
                    //todo oggettojson
                    managePendingFriends(tokens[2]);
                    break;
                case "SFIDA":
                    try {
                        manageSfida(tokens[2], tokens[3], tokens[4]);
                    }catch(ArrayIndexOutOfBoundsException e){
                        manageSfida(tokens[2], tokens[3], null);
                    }

                    break;


                default:

                    break;
            }

        }

        else{
            //boh exception?
        }

    }

    /**
     * invia la sfida ad un amico
     * @param friend
     * @return true se accettata, false altrimenti
     */
    public void inviaSfida(String friend){
        String request="SFIDA "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RICHIEDI;
        send(request);

    }

    private void manageSfida(String friend, String type, String p) {
        //todo per accettare vado nella pagina delle richieste??

        switch(Settings.SFIDA.valueOf(type)){
            case ACCETTATA:

                break;
            case RIFIUTATA:
            case SCADUTA:
                synchronized (richiesteSfida) {
                    if(richiesteSfida.remove(friend)!=null)
                        richiesteSfida.notify();
                }
                break;
            case INIZIATA:

                synchronized (sfida) {
                    if (sfida.isEmpty()) {
                        sfida.add(friend);
                        sfida.add(p);
                        sfida.notify();
                    }
                }

                break;
            case TERMINATA:
                synchronized (sfida) {
                    if(sfida.size()>0) {
                        sfida.add(p);
                        sfida.notify();
                    }
                }
                break;
        }

    }

    private void manageAmici(String json) {
        try {
            JSONArray array=(JSONArray) (new JSONParser().parse(json));
            synchronized (friendsList){
                friendsList.clear();
                for (Object s:array) {

                    friendsList.add((String) s);
                }
                friendsList.notify();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void manageClassifica(String json) {
        try {
            JSONArray array=(JSONArray) (new JSONParser().parse(json));
            synchronized (classificaList) {
                classificaList.clear();
                for (Object s : array) {
                    //gui.addPendingFriendTile((String) s);
                    JSONObject amicoJSON=(JSONObject)s;
                    String amico=amicoJSON.get("nick").toString() + amicoJSON.get("score").toString();
                    classificaList.add(amico);
                }
                classificaList.notify();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void managePendingFriends(String json) {
        try {
            JSONArray array=(JSONArray) (new JSONParser().parse(json));
            //gui.setPendingRow(array.size());
            synchronized (pendingFriendsList) {
                pendingFriendsList.clear();
                for (Object s : array) {
                    //gui.addPendingFriendTile((String) s);
                    pendingFriendsList.add((String) s);
                }
                pendingFriendsList.notify();
            }
            //gui.boh();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }




    /**
     * Invia la richiesta di amicizia
     */
    public String aggiungiAmico(String friend){
        String request= Settings.REQUEST.AMICIZIA+" " +loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RICHIEDI;
        send(request);
        String response=getResponse();
        //potrebbe averla accettata direttamente
        String tokens[]=response.split(" ");

        if(tokens.length>3 && tokens[1].equals("AMICIZIA") && tokens[4].equals("ACCETTATA")) manageAmicizia(tokens[3], tokens[4]);
        return response;

        //response OK
        //response NOK eccezione
    }

    /**
     * accetta la richiesta di amicizia
     * @param friend
     */
    public boolean accettaAmico(String friend) {

        String request= Settings.REQUEST.AMICIZIA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.ACCETTA;
        send(request);
        String[] tokens=getResponse().split(" ");
        if(tokens[0].equals("OK")){
            synchronized (friendsList){
                synchronized (pendingFriendsList){
                    pendingFriendsList.remove(friend);

                    pendingFriendsList.notify();

                }
                friendsList.add(friend);
                friendsList.notify();
            }
            getClassifica();
            return true;
        }
        return false;
    }

    private void getClassifica() {
        String request="GET "+loggedNick+" "+token+" CLASSIFICA";
        send(request);
    }

    public boolean rifiutaAmico(String friend) {
        String request="AMICIZIA "+loggedNick+" "+token+" "+friend+" RIFIUTA";
        send(request);
        String[] tokens=getResponse().split(" ");
        if(tokens[0].equals("OK")){

            synchronized (pendingFriendsList){
                pendingFriendsList.remove(friend);
                pendingFriendsList.notify();

            }


            return true;
        }
        return false;
    }



    /**
     * Richiede le informazioni di tipo type sull'utente
     */
    public void getRequest(Settings.GetType type){
        String request = Settings.REQUEST.GET+" "+loggedNick+" "+token+" "+type;
        send(request);
        //todo server return json object
        //response OK LIST ...
        //response NOK eccezione

    }

    /**
     * gestisce la richiesta di amicizia (stampa solo info sulla richiesta)
     * @param friend
     * @param type
     * @return
     */
    public String manageAmicizia(String friend, String type){
        //todo per accettare vado nella pagina delle richieste??
        switch(type){
            case "RICHIESTA":

                //todo show richiesta arrivata
                //System.out.println("Richiesta di amicizia da "+friend);
                synchronized (pendingFriendsList) {
                    pendingFriendsList.add(friend);
                    pendingFriendsList.notify();
                }
                //todo gui.addPendingFriendTile(friend);

                break;
            case "ACCETTATA":
                synchronized (pendingFriendsList) {
                    if(pendingFriendsList.remove(friend))
                        pendingFriendsList.notify();
                }
                synchronized (friendsList) {
                    friendsList.add(friend);
                    friendsList.notify();
                }
                //get classifica
                getRequest(Settings.GetType.CLASSIFICA);
                //todo gui.addFriendTile(friend);
                break;
            case "RIFIUTATA":
                //todo non mi interessano le richieste rifiutate, rimuoverlo anche dal server
                break;
        }
        return null;
    }


    public void setToken(String token) {
        this.token=token;
    }


    public void setLoggedNick(String nick){
        loggedNick=nick;
    }

    /**
     * Restituisce la nuova parola o il punteggio se la sfida è terminata
     * @param traduzione
     * @return
     */
    public String inviaTraduzione(String traduzione) throws Exception{
        String request=Settings.REQUEST.PAROLA+" "+loggedNick+" "+token+" "+traduzione;
        send(request);
        String response=getResponse();
        String[] tokens=response.split(" ");
        if(Settings.RESPONSE.valueOf(tokens[0]).equals(Settings.RESPONSE.OK)){
            if(Settings.RESPONSE.valueOf(tokens[1]).equals(Settings.RESPONSE.SFIDA)
                && Settings.SFIDA.valueOf(tokens[4]).equals(Settings.SFIDA.TERMINATA)){
                synchronized (sfida) {
                    if(sfida.size()>0) {
                        sfida.add(tokens[5]);
                        sfida.notify();
                    }
                }
                return tokens[5];
            }
            else if(Settings.RESPONSE.valueOf(tokens[1]).equals(Settings.RESPONSE.PAROLA)){
                return tokens[3];
            }
        }
        throw new Exception(response);
    }

    public void accettaSfida(String friend) {
        String request=Settings.RESPONSE.SFIDA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.ACCETTA;
        send(request);
        synchronized (richiesteSfida){

            richiesteSfida.remove(friend);
            richiesteSfida.notify();
        }


    }

    public void rifiutaSfida(String friend) {
        String request=Settings.RESPONSE.SFIDA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RIFIUTA;
        send(request);

        synchronized (richiesteSfida){

            richiesteSfida.remove(friend);
            richiesteSfida.notify();
        }


    }
}

