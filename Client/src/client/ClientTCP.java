package client;

import Settings.Settings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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
    private Scanner scanner;
    private String token;
    private StringBuilder lastResponse;
    private String loggedNick;
    //Oggetti su cui notificare alla GUI
    private List<String> sfida;
    private ConcurrentHashMap<String, String> richiesteSfida;
    private List<String> pendingFriendsList;
    private List<String> friendsList;
    private List<String> classificaList;

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
            byteBuffer=ByteBuffer.allocate(Settings.TCPBUFFLEN);
            while (!socketChannel.finishConnect()) {
                System.out.println("Non terminata la connessione");
            }
            System.out.println("Terminata la connessione");

            scanner = new Scanner(new InputStreamReader(socketChannel.socket().getInputStream(), StandardCharsets.UTF_8));

        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    public void setToken(String token) {
        this.token=token;
    }

    public void setLoggedNick(String nick){
        loggedNick=nick;
    }

    public String getLoggedNick() {
        return loggedNick;
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
            System.out.println("server.Server Disconnected");
        }
    }

    /**
     * Gestisce il comando ricevuto tramite switch sui metodi interni
     */
    public void manageCommand(String response){
        String[] tokens=response.split(" ");
        //quelli che iniziano con OK NOK sono in risposta a richieste
        if ((Settings.RESPONSE.valueOf(tokens[0]).equals(Settings.RESPONSE.OK)
                    || Settings.RESPONSE.valueOf(tokens[0]).equals(Settings.RESPONSE.NOK) )
                && tokens[2].equals(token)) {
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
     * @return l'ultima risposta ricevuta
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

    /*                      INVIO AMICIZIA                          */

    /**
     * Invia la richiesta di amicizia
     * @param friend a chi voglio inviarla
     * @return stringa di risposta del server
     * @throws IOException
     */
    public String aggiungiAmico(String friend) throws IOException{
        String request= Settings.REQUEST.AMICIZIA+" " +loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RICHIEDI;
        send(request);
        return getResponse();
    }

    /*                      RICHIESTE AMICIZIA                      */

    /**
     * Invia al server il comando per accettare la richiesta di amicizia
     * @param friend chi voglio accettare
     * @return true se è stata accettata, false altrimenti
     */
    public boolean accettaAmico(String friend) throws IOException{
        String request= Settings.REQUEST.AMICIZIA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.ACCETTA;
        send(request);
        String[] tokens=getResponse().split(" ");
        if(Settings.RESPONSE.valueOf(tokens[0]).equals(Settings.RESPONSE.OK)){
            synchronized (friendsList){
                synchronized (pendingFriendsList){
                    pendingFriendsList.remove(friend);
                    pendingFriendsList.notify();
                }
                friendsList.add(friend);
                friendsList.notify();
            }
            return true;
        }
        return false;
    }

     /**
     * Invia il comando per rifiutare una richiesta di amicizia
     * @param friend chi voglio rifiutare
     * @return true se è stata rifiutata, false altrimenti
     * @throws IOException
     */
    public boolean rifiutaAmico(String friend) throws IOException{
        String request=Settings.REQUEST.AMICIZIA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RIFIUTA;
        send(request);
        String[] tokens=getResponse().split(" ");
        if(Settings.RESPONSE.valueOf(tokens[0]).equals(Settings.RESPONSE.OK)){

            synchronized (pendingFriendsList){
                pendingFriendsList.remove(friend);
                pendingFriendsList.notify();

            }
            return true;
        }
        return false;
    }

    /*                      SFIDA                                   */

    /**
     * invia la sfida ad un amico
     * @param friend
     * @return true se accettata, false altrimenti
     */
    public String inviaSfida(String friend) throws IOException{
        String request=Settings.REQUEST.SFIDA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RICHIEDI;
        send(request);
        return getResponse();

    }

    /**
     * Invia il comando per accettare la sfida e la rimuove dalla lista
     * @param friend chi voglio accettare
     * @return risposta del server
     * @throws IOException
     */
    public String accettaSfida(String friend) throws IOException{
        String request=Settings.REQUEST.SFIDA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.ACCETTA;
        send(request);
        synchronized (richiesteSfida){
            richiesteSfida.remove(friend);
            richiesteSfida.notify();
        }
        return getResponse();

    }

    /**
     * Invia il comando per rifiutare la sfida e la rimuove dalla lista
     * @param friend chi voglio rifiutare
     * @throws IOException
     */
    public void rifiutaSfida(String friend) throws IOException{
        String request=Settings.REQUEST.SFIDA+" "+loggedNick+" "+token+" "+friend+" "+Settings.RQTType.RIFIUTA;
        send(request);
        synchronized (richiesteSfida){
            richiesteSfida.remove(friend);
            richiesteSfida.notify();
        }
    }

    /*                      TRADUZIONI                              */

    /**
     * Effettua l'invio della traduzione della parola
     * @param traduzione la parola tradotta
     * @return la nuova parola se la sfida non è terminata oppure il punteggio se termina
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

    /**                     METODI PER LA GESTIONE DELLE RISPOSTE   */

    /*                      SFIDE                                   */

    /**
     * Gestisce i comandi ricevuti per la sfida
     * @param friend a chi era riferita la sfida
     * @param type tipo di azione sulla sfida
     * @param p parola o punteggio
     */
    private void manageSfida(String friend, String type, String p) {
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

    /*                      RICHIESTE DI AMICIZIA                   */

    /**
     * Gestisce la risposta ad un'amicizia inviata (stampa solo info sulla richiesta)
     * Aggiorna la lista delle amicizie in caso di richiesta accettata e rimuove la richiesta
     * @param friend amico a cui si vuole inviare
     * @param type ENUM AMICIZIA
     */
    public void manageAmicizia(String friend, String type){
        switch(Settings.AMICIZIA.valueOf(type)){
            case RICHIESTA:

                //todo show richiesta arrivata
                //System.out.println("Richiesta di amicizia da "+friend);
                synchronized (pendingFriendsList) {
                    pendingFriendsList.add(friend);
                    pendingFriendsList.notify();
                }
                //todo gui.addPendingFriendTile(friend);

                break;
            case ACCETTATA:
                synchronized (pendingFriendsList) {
                    if(pendingFriendsList.remove(friend))
                        pendingFriendsList.notify();
                }
                synchronized (friendsList) {
                    friendsList.add(friend);
                    friendsList.notify();
                }
                //Aggiorno la classifica
                getRequest(Settings.GetType.CLASSIFICA);

                //todo gui.addFriendTile(friend);
                break;
            case RIFIUTATA:
                //todo non mi interessano le richieste rifiutate, rimuoverlo anche dal server
                break;
        }
    }

    /**
     * Effettua il parse del json con le richieste di amicizia aggiornando la lista
     * @param json stringa JSON contenente le richieste di amicizia
     */
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

    /*                      CLASSIFICA                              */

    /**
     * Effettua il parse del json con la classifica e aggiorna la lista
     * @param json json da parsare
     */
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

    /*                      AMICI                                   */

    /**
     * Effettua il parse del json contenente gli amici e aggiorna la lista
     * @param json json da parsare
     */
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

    }

    /*                      LETTURA/SCRITTURA                       */

    public String read() throws NoSuchElementException{
        String response=scanner.nextLine();
        while(response==null) {
            response = scanner.nextLine();
        }
        return response;
    }

    public void send(String message) throws IOException {
        //todo aggiusta
        System.out.println("REQUEST: "+message);
        ByteBuffer buf = ByteBuffer.wrap((message+"  \n").getBytes());

        while (buf.hasRemaining()) {
            socketChannel.write(buf);
        }

    }


}

