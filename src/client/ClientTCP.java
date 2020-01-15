package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.ServerUdp;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

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

    private StringBuilder lastResponse;
    private String loggedNick;

    private ServerUdp udp;

    public ClientTCP(List<String> pendingFriendsList, List<String> friendsList, List<String> classificaList){
        this.pendingFriendsList=pendingFriendsList;
        this.friendsList=friendsList;
        this.classificaList=classificaList;
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
    public String login(String nick, String pw){
        String message="LOGIN "+nick+" "+pw;
        send(message);
        String response=read();
        return response;
    }


    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){

            String response=read();
            System.out.println("r"+response);
            manageCommand(response);
        }
    }

    /**
     * restituisce l'ultima risposta a un comando
     * @return
     */
    public synchronized String getResponse(){

        synchronized (lastResponse){
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
        if (tokens[0].equals("OK") || tokens[0].equals("NOK")) {
            synchronized (lastResponse) {
                lastResponse.append(response);
                lastResponse.notify();
            }
        }

        else if(tokens[1].equals(token)) { //validazione tramite token della risposta


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
                    //manageSfida();
                    break;


                default:

                    break;
            }

        }

        else{
            //boh exception?
        }

    }

    private void manageAmici(String json) {
        try {
            JSONArray array=(JSONArray) (new JSONParser().parse(json));
            synchronized (friendsList){
                for (Object s:array) {
                //TODO NOTIFY AMICI
                //gui.addFriendTile((String)s);

                    friendsList.add((String) s);
                }
                friendsList.notify();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void manageClassifica(String json) {
        //todo parse json and update gui
        try {
            JSONArray array=(JSONArray) (new JSONParser().parse(json));
            synchronized (classificaList) {
                for (Object s : array) {
                    //gui.addPendingFriendTile((String) s);
                    JSONObject amicoJSON=(JSONObject)s;
                    String amico=amicoJSON.get("nick").toString() + amicoJSON.get("punteggio").toString();
                    if(!classificaList.contains(amico))classificaList.add(amico);
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
    public void aggiungiAmico(String friend){
        //todo get from tb
        String request="AMICIZIA "+loggedNick+" "+token+" "+friend+" RICHIESTA";
        send(request);
        String response=getResponse();

        //response OK
        //response NOK eccezione
    }

    /**
     * accetta la richiesta di amicizia
     * @param friend
     */
    public boolean accettaAmico(String friend) {

        String request="AMICIZIA "+loggedNick+" "+token+" "+friend+" ACCETTA";
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
            //todo aggiorna classifica

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
     * invia la sfida ad un amico
     * @param friend
     * @return true se accettata, false altrimenti
     */
    public boolean inviaSfida(String friend){
        String request="SFIDA "+loggedNick+" "+token+" "+friend;
        send(request);
        //response OK AMICIZIA ACCETTATA/RIFIUTATA
        //response NOK eccezione
        String[] tokens=getResponse().split(" ");
        return tokens[0].equals("OK");

    }

    /**
     * Richiede la lista delle richieste di amicizia in sospeso
     */
    public void getPendingFriends(String nick, String token){
        String request = "GET "+nick+" "+token+" PENDING FRIENDS";
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
                synchronized (friendsList) {
                    friendsList.add(friend);
                    friendsList.notify();
                }
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



}

