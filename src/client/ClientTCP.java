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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Classe che implementa tutti i metodi per la gestione della parte tcp del server
 *
 * Per l'invio di comandi al server si invocano le funzioni sull'oggetto
 *
 * Quando il thread viene avviato è bloccato sulla read, è possibile ricevere due tipi di risposte dal server::
 * 1)Risposte sincrone: risposte ai comandi inviati dal client
 *                      è possibile accedere all'ultima risposta ricevuta con il metodo getResponse()
 * 2)Risposte asincrone: risposte che il server invia per notificare eventi
 *                       Per le risposte asincrone (Aggiornamenti su richieste di amicizia, amicizie, classifica, sfide)
 *                          utilizza delle variabili condivise sulle quali effettua le varie operazioni richieste dal
 *                          server
 *
 *
 */
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
    private ConcurrentHashMap<String, String> richiesteSfida;
    private StringBuilder lastResponse;
    private String loggedNick;
    private List<String> sfida;

    public ClientTCP(List<String> sfida,
                     List<String> pendingFriendsList,
                     List<String> friendsList,
                     List<String> classificaList,
                     ConcurrentHashMap<String, String> richiesteSfida){
        this.sfida=sfida;
        this.pendingFriendsList=pendingFriendsList;
        this.friendsList=friendsList;
        this.classificaList=classificaList;
        this.richiesteSfida=richiesteSfida;
        loggedNick=null;
        token=null;
        address = new InetSocketAddress("127.0.0.1", 8080);
        lastResponse=new StringBuilder();

        try {
            socketChannel = SocketChannel.open();
            //socketChannel.configureBlocking(false);
            socketChannel.connect(address);
            byteBuffer=ByteBuffer.allocate(BUFFLEN);
            while (!socketChannel.finishConnect()) {
                System.out.println("Non terminata la connessione");
            }
            System.out.println("Terminata la connessione");

            scanner = new Scanner(new InputStreamReader(socketChannel.socket().getInputStream(), StandardCharsets.UTF_8));

        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    /**
     * @return true se si è loggati, false altrimenti
     */
    public boolean isLogged(){
        return loggedNick!=null;
    }

    /**
     * Effettua la richiesta di login dell'utente
     * @param nick utente
     * @param pw password
     * @return stringa di risposta del server
     */
    public String login(String nick, String pw, int udpPort) throws IOException{
        String message=Settings.RESPONSE.LOGIN+" "+nick+" "+pw+" "+udpPort;
        send(message);
        String response=read();
        String[] tokens=response.split(" ");
        if(Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))){
            loggedNick=nick;
            token=tokens[2];
        }
        return response;
    }

    /**
     * Effettua la richiesta di logout dell'utente
     */
    public void logout() {
        try{
            String request=Settings.REQUEST.LOGOUT+" "+loggedNick+" "+token;
            send(request);
        }catch(IOException ex){}

    }

    @Override
    public void run() {
        try{
            while (!Thread.currentThread().isInterrupted()) {
                String response = read();
                System.out.println("RESPONSE: " + response);
                manageCommand(response);
            }
        }catch (NoSuchElementException e){
            System.out.println("Server Disconnected");
        }
    }

    /**
     * Gestisce il comando ricevuto
     * @param response
     */
    public void manageCommand(String response){
        String[] tokens=response.split(" ");
        //quelli che iniziano con OK NOK sono in risposta a richieste
        if ((tokens[0].equals("OK") || tokens[0].equals("NOK")) && tokens[2].equals(token)) {
            synchronized (lastResponse) {
                lastResponse.setLength(0);
                lastResponse.append(response);
                lastResponse.notify();
            }
        }
        else if(tokens[1].equals(token)) { //validazione tramite token della risposta
            //ASYNC
            switch (Settings.RESPONSE.valueOf(tokens[0])) {
                //AMICIZIA TOKEN NICK TYPE
                case AMICIZIA:
                    manageAmicizia(tokens[2], tokens[3]);
                    break;
                case AMICI:
                    //todo oggettojson
                    manageAmici(tokens[2]);
                    break;
                case CLASSIFICA: //async
                    //todo oggettojson classifica
                    manageClassifica(tokens[2]);
                    break;
                case PENDING:
                    //todo oggettojson
                    managePendingFriends(tokens[2]);
                    break;
                case SFIDA:
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
     * invia la sfida ad un amico
     * @param friend
     * @return true se accettata, false altrimenti
     */
    public String inviaSfida(String friend) throws IOException{
        String request="SFIDA "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RICHIEDI;
        send(request);
        return getResponse();

    }

    private void manageSfida(String friend, String type, String p) {
        //todo per accettare vado nella pagina delle richieste??

        switch(Settings.SFIDA.valueOf(type)){
            case ACCETTATA:
                synchronized (sfida) {
                    if (sfida.isEmpty()) {
                        sfida.add(friend);
                        sfida.notify();
                    }
                }
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
                    if(sfida.isEmpty()) {
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
    public String aggiungiAmico(String friend) throws IOException{
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
    public boolean accettaAmico(String friend) throws IOException{

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

    private void getClassifica() throws IOException{
        String request="GET "+loggedNick+" "+token+" CLASSIFICA";
        send(request);
    }

    public boolean rifiutaAmico(String friend) throws IOException{
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
    public void getRequest(Settings.GetType type)  {
        String request = Settings.REQUEST.GET+" "+loggedNick+" "+token+" "+type;
        try {
            send(request);
        }
        catch (IOException ignored){

        }

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
    public String inviaTraduzione(String traduzione) throws IllegalArgumentException, IOException{
        String request=Settings.REQUEST.PAROLA+" "+loggedNick+" "+token+" "+traduzione;
        send(request);
        String response=getResponse();
        String[] tokens=response.split(" ");
        if(Settings.RESPONSE.valueOf(tokens[0]).equals(Settings.RESPONSE.OK)){
            if(Settings.RESPONSE.valueOf(tokens[1]).equals(Settings.RESPONSE.SFIDA)
                && Settings.SFIDA.valueOf(tokens[4]).equals(Settings.SFIDA.TERMINATA)){
                synchronized (sfida) {
                    if(sfida.isEmpty()) {
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
        throw new IllegalArgumentException(response);
    }

    public String accettaSfida(String friend) throws IOException{
        String request=Settings.REQUEST.SFIDA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.ACCETTA;
        send(request);
        synchronized (richiesteSfida){

            richiesteSfida.remove(friend);
            richiesteSfida.notify();
        }
        return getResponse();


    }

    public void rifiutaSfida(String friend) throws IOException{
        String request=Settings.REQUEST.SFIDA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RIFIUTA;
        send(request);

        synchronized (richiesteSfida){

            richiesteSfida.remove(friend);
            richiesteSfida.notify();
        }


    }

    public String read() throws NoSuchElementException{
        String response=scanner.nextLine();
        while(response==null) {
            response = scanner.nextLine();
        }
        return response;
    }

    public void send(String message) throws IOException, AsynchronousCloseException {
        //todo aggiusta
        System.out.println("REQUEST: "+message);
        ByteBuffer buf = ByteBuffer.wrap((message+"  \n").getBytes());

        while (buf.hasRemaining()) {
            socketChannel.write(buf);
        }

        buf.clear();
    }

    public String getLoggedNick() {
        return loggedNick;
    }
}

