package client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Scanner;

public class ClientTCP implements Runnable{
    private SocketAddress address;
    private SocketChannel socketChannel;
    private ByteBuffer byteBuffer;
    private int BUFFLEN=1024;
    private Scanner scanner;
    private ClientLoggedGUI clientLoggedGUIGUI;
    public ClientTCP(){
        address = new InetSocketAddress("127.0.0.1", 8080);


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
        StringBuilder msg = new StringBuilder();

        String response="";

         response = scanner.nextLine();

        return response;


        /*int read = 0;
        try {
            read = socketChannel.read(byteBuffer);

        while (read > 0 ) {

            String ne=new String(Arrays.copyOfRange(byteBuffer.array(), 0, read));
            msg.append(ne);
            byteBuffer.clear();
            //System.out.println(ne+read);
            read = socketChannel.read(byteBuffer);

            //
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg.toString();*/

    }

    public void send(String message){
        //todo aggiusta sta merda
        ByteBuffer buf = ByteBuffer.wrap(message.getBytes());
        //System.out.println(message);
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
        while(response==null||response.length()==0){
            response=read();
        }
        return response;
    }

    /*public String readBlocking() {
        StringBuilder msg = new StringBuilder();

        String response="";

        response = nb.getLineBlocking();

        return response;
    }*/

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            String response=read();
            manageResponse(response);
        }
    }

    public void manageResponse(String response){
        //todo cambiare risposte server
        String[] tokens=response.split(" ");
        if(tokens[0].equals("OK")){
            switch(tokens[1]){
                case "AMICIZIA":
                    manageAmicizia();
                    break;
                case "SFIDA":
                    //manageSfida();
                    break;
                case "LIST":
                    managePendingFriends(tokens);
                    break;
            }
        }
        else if(tokens[0].equals("NOK")){

        }
        else{

        }

        //if(){}
    }

    /**
     * Invia la richiesta di amicizia
     */
    public void aggiungiAmico(){
        //todo get from tb
        String friend="";//nbIO.getLineBlocking();
        String request="AMICIZIA "+loggedNick+" "+token+" "+friend+" RICHIESTA";
        send(request);
        //response OK
        //response NOK eccezione
    }

    /**
     * gestisce l'invio della sfida ad un amico
     */
    public void inviaSfida(){
        System.out.println("Inserisci il nome dell'amico da sfidare");
        String friend=nbIO.getLineBlocking();
        String request="SFIDA "+loggedNick+" "+token+" "+friend;
        send(request);
        //response OK AMICIZIA ACCETTATA/RIFIUTATA
        //response NOK eccezione
    }

    /**
     * Richiede la lista delle richieste di amicizia in sospeso
     */
    public void getPendingFriends(){
        String request = "GET "+loggedNick+" "+token+" PENDING FRIENDS";
        tcp.send(request);
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
                System.out.println("Richiesta di amicizia da "+friend);
                pendingSize++;
                //manageRichiestaAmicizia(friend);
                break;
            case "ACCETTATA":
                //boh
                break;
            case "RIFIUTATA":
                //boh
                break;
        }
        return null;
    }



    public void managePendingFriends(String[] friends){
        System.out.println("0: INDIETRO");
        for (int i = 2; i < friends.length; i++) {
            System.out.println(i + ": " + friends[i]);
        }
        //todo gestione nuove richieste e sfide
        int scelta=-1;
        while(scelta<0 /*&& !udp request sfida*/) {
            String line;
            if((line=nbIO.getLine())!=null){
                scelta=Integer.parseInt(line);

                if(friends!=null && scelta>0 && scelta<=friends.length) {
                    //managePendingFriend(friends[scelta - 1]);
                    if(scelta>0) {
                        System.out.println("0) INDIETRO");
                        System.out.println("1) ACCETTA");
                        System.out.println("2) RIFIUTA");
                    }
                }

            }

        }


    }

    public void setGUI(ClientLoggedGUI _clientLoggedGUI){

        clientLoggedGUIGUI=_clientLoggedGUI;
    }
}

