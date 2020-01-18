package server;

import exceptions.*;

import Settings.Settings;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

//todo wait for reply (cosi non si accodano le risposte)
public class WorkerTCP implements Runnable {

    private SelectionKey k;
    private int BUFFLEN=1024;
    private Selector selector;
    private Users users;
    private final ConcurrentHashMap<SelectionKey, SelectionKey> usingK;
    //private ConcurrentHashMap<String, SelectionKey> keys;
    private ConcurrentHashMap<String, SelectionKey> keys; //contiene Selection Key e UDP port
    private String token;
    public WorkerTCP(Selector s, SelectionKey k, Users users,
                     ConcurrentHashMap<SelectionKey, SelectionKey> usingK,
                     ConcurrentHashMap<String, SelectionKey> keys) {
        selector=s;
        this.k=k;
        this.usingK = usingK;
        this.users = users;
        this.keys=keys;
        token=null;
    }

    public void run() {
        try {
            if (k.isAcceptable()) { //se è il channel sul quale accetto le connessioni
                accept(k);
            } else if (k.isReadable()) {
                String command = read(k);
                if(command.length()>0) {
                    System.out.println("REQUEST: " + command);
                    String response = null;
                    try {
                        response = manageCommand(command, k);

                    } catch (Exception e) {
                        e.printStackTrace();
                        response = Settings.RESPONSE.NOK +" " + e.toString().split(":")[0]+ " "+token;
                    } finally {
                        if (response != null) {
                            System.out.println("Response: " + response);
                            send(k, response + "\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Disconnected");
            //todo cancellare sfide attive, logout, cancellare token
            /*String nick=keys.keySet()
                            .stream()
                            .filter(key -> k.equals(keys.get(key).getFirst()))
                            .findFirst().get();*/
            k.cancel();
            logout(((MyAttachment)k.attachment()).getNick());

        }
        finally {
            synchronized (usingK) {
                usingK.remove(k);
            }
        }

    }

    public String manageCommand(String cmd, SelectionKey k) throws FriendshipException, UserNotExists, WrongCredException, UserAlreadyLogged, UserNotOnline, UserAlreadyInGame, ChallengeException {
        //todo mandare la porta udp al login
        String[] tokens=cmd.split(" ");
        token=tokens[2];
        Settings.REQUEST r = Settings.REQUEST.valueOf(tokens[0]);
        switch(r){
            case LOGIN:
                return Settings.RESPONSE.OK+" "+login(tokens, k);
            case SFIDA:
                //todo update della classifica per tutti i client amici
                return sfida(tokens);
            case AMICIZIA:
                return Settings.RESPONSE.OK+" "+amicizia(tokens);
            case GET:
                return get(tokens);
            case PAROLA:
                return Settings.RESPONSE.OK+" "+word(tokens);

            /*case "PAROLA":
                //parola nick token parola
                //return parola();
                break;*/
        }

        return null;
    }

    /**
     * Gestisce l'ivio di una parola della sfida
     * @return
     */
    private String word(String[] tokens) throws UserNotExists, ChallengeException {
        if(users.validateToken(tokens[1], tokens[2])){
            Challenge c=users.getChallenge(tokens[1]);
            if(c==null) throw new ChallengeException();
            String nextWord=c.tryWord(tokens[1], tokens[3]);
            if(nextWord!=null){
                return Settings.RESPONSE.PAROLA+" "+tokens[2]+" "+nextWord;
            }
            else{ //sfida terminata
                //todo inoltrare all'avversario
                users.terminaSfida(c);
                String friend=c.getOpponent(tokens[1]);

                inoltraTermineSfida(tokens[1], friend, Settings.SFIDA.TERMINATA, c.getScore(friend));
                return Settings.RESPONSE.SFIDA+" "+token+" "+friend+" "+Settings.SFIDA.TERMINATA+" "+c.getScore(tokens[1])+" \n"+
                        Settings.RESPONSE.CLASSIFICA+" "+ token+" "+ users.mostraClassifica(tokens[1]).toJSONString();
            }
        }
        throw new WrongCredException();
    }

    public void inoltraTermineSfida(String nick, String friend, Settings.SFIDA type, int punteggio) throws UserNotExists {
        String request=Settings.RESPONSE.SFIDA+" "+ users.getToken(friend)+" "+nick+" "+type+" "+punteggio+" \n"+
                Settings.RESPONSE.CLASSIFICA+" "+ users.getToken(friend)+" "+ users.mostraClassifica(nick).toJSONString();
        SelectionKey k= keys.get(friend);
        if(k==null) return ; //se non è registrata la key non la inoltro (non è online)
        //try {
            /*synchronized (usingK) {
                while (usingK.putIfAbsent(k, k) != null) {
                    usingK.wait();
                }
            }*/

            try {
                send(k, request + "\n");
            }catch (IOException e){

            }

            /*synchronized (usingK) {
                usingK.remove(k);
                usingK.notify();
            }*/

        /*} catch (InterruptedException e) {
            e.printStackTrace();

        }*/
    }

    /**
     * Gestisce la richiesta di login
     * @param k selectionkey associata
     * @param tokens request string tokenizzata
     * @return
     */
    public String login(String[] tokens, SelectionKey k)throws WrongCredException, UserNotExists, UserAlreadyLogged{
        String token=users.login(tokens[1], tokens[2]);
        Integer port=Integer.valueOf(tokens[3]);
        keys.put(tokens[1], k);
        ((MyAttachment)k.attachment()).setNick(tokens[1]).setUDPPort(port);
        return  Settings.RESPONSE.LOGIN+" "+token+" \n"+
                Settings.RESPONSE.AMICI+" "+ token+" "+ users.listaAmici(tokens[1]).toJSONString()+" \n"+
                Settings.RESPONSE.PENDING+" "+ token+" "+ users.listaRichieste(tokens[1]).toJSONString()+" \n" +
                Settings.RESPONSE.CLASSIFICA+" "+ token+" "+ users.mostraClassifica(tokens[1]).toJSONString();

    }

    public void logout(String nick){
        users.logout(nick);
    }

    /**
     * Crea la sfida tra i due users inoltrando la richiesta udp all'altro
     * @param tokens
     * @return true false
     */
    public String sfida(String[] tokens) throws UserNotOnline, UserNotExists, UserAlreadyInGame {
        if (users.validateToken(tokens[1], tokens[2])){
            switch (Settings.RQTType.valueOf(tokens[4])) {
                case RICHIEDI:
                    //todo accettarla direttamente se già richiesta??
                    try {
                        if (inoltraRichiestaSfida(tokens[1], tokens[3])) {
                            Challenge c = users.sfida(tokens[1], tokens[3]);
                            //schedulo il timer di chiusura sfida
                            Timer t = new Timer();
                            TimerTask tt = new TimerChallenge(keys.get(tokens[1]), keys.get(tokens[3]), c, users);
                            t.schedule(tt, Settings.timer);

                            //todo send ad entrambi sfida iniziata
                            //todo usingk
                            send(keys.get(tokens[3]), Settings.RESPONSE.SFIDA + " " + users.getToken(tokens[3]) +
                                    " " + tokens[1] + " " + Settings.SFIDA.INIZIATA+" "+c.getWord(0)+"\n");
                            return Settings.RESPONSE.SFIDA + " " + tokens[2] + " " + tokens[3] + " " + Settings.SFIDA.INIZIATA+" "+c.getWord(0);

                        } else {


                        }
                    } catch (IOException e) {
                        throw new UserNotOnline();
                    }
             }
             throw  new IllegalArgumentException();
        }
        else
            throw new WrongCredException();
    }



    /**
     * Inoltra la sfida su udp a friend
     * @return true se la accetta, false se la rifiuta
     */
    public boolean inoltraRichiestaSfida(String nick, String friend) throws IOException, UserNotExists, UserNotOnline {
        if(!users.isLogged(friend)) throw new UserNotOnline();
        DatagramSocket udpClient = new DatagramSocket(8081);
        udpClient.setSoTimeout(Settings.UDP_TIMEOUT);

        InetAddress address = InetAddress.getByName(Settings.HOST_NAME);
        String msg = Settings.RESPONSE.SFIDA+" "+friend+" "+
                users.getToken(friend)+" "+nick+" \n";
        byte[] msgSfida = msg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(msgSfida, msgSfida.length, address,
                ((MyAttachment)keys.get(friend).attachment()).getUDPPort());
        udpClient.send(packet);

        byte[] ack = new byte[1024];
        DatagramPacket rcvPck = new DatagramPacket(ack, ack.length);

        try {
            //todo validate response
            udpClient.receive(rcvPck);
            msg = new String(rcvPck.getData());
        } catch (SocketTimeoutException e) {
            //e.printStackTrace();
            udpClient.close();
            return false;
        }
        //todo manage response
        System.out.println("UDP RESPONSE:"+msg);
        udpClient.close();


        return true;
    }

    public String amicizia(String[] tokens) throws FriendshipException, WrongCredException, UserNotExists {
        if (users.validateToken(tokens[1], tokens[2])){
            Settings.RQTType type= Settings.RQTType.valueOf(tokens[4]);
            String response=Settings.RESPONSE.AMICIZIA+" "+users.getToken(tokens[1])+" "+tokens[3]+" ";
            switch(type){
                case RICHIEDI:
                    if(richiediAmicizia(tokens[1], tokens[3])) {
                        response += Settings.RSPType.RICHIESTA;
                    }
                    else{
                        response+=Settings.RSPType.ACCETTATA;
                    }
                    break;
                case ACCETTA:
                    accettaAmicizia(tokens[1], tokens[3]);
                    response+=Settings.RSPType.ACCETTATA;
                    break;
                case RIFIUTA:
                    //rimuovo dalla pending
                    users.removePending(tokens[1], tokens[3]);
                    response+=Settings.RSPType.RIFIUTATA;
                    break;
                default:
                    break;
            }
            return response;
        }
        else{
            throw new WrongCredException();
        }
    }


    /**
     * inoltra la richiesta di amicizia a friend
     * @param nick
     * @param friend
     * @return false se accetta direttamente l'amicizia perchè già nei pending
     */
    public boolean richiediAmicizia(String nick, String friend) throws FriendshipException, UserNotExists {
        //todo
        //todo inoltra la richiesta all'altro
        if(users.exists(friend) && !friend.equals(nick)) {
            if(users.addPending(nick, friend)) { //aggiungo nick ai pending di friend
                inoltraAmicizia(nick, friend,  Settings.RSPType.RICHIESTA);
                return true;
            }
            else{   //se invece era già presente accetto l'amicizia direttamente
                accettaAmicizia(nick, friend);
                return false;
            }
        }
        else{
            throw new UserNotExists();
        }
    }

    /**
     * Acccetta l'amicizia e la inoltra
     * @param nick
     * @param friend
     */
    public void accettaAmicizia(String nick, String friend) throws UserNotExists {
        //l'altro ha accettato, crea il legame e inoltra l'amicizia
        //todo controllare se esiste legame
        if(users.containsPending(nick, friend)) { //
            users.aggiungiAmico(nick, friend);
            inoltraAmicizia(nick, friend, Settings.RSPType.ACCETTATA);
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    /**
     * inoltra amicizia da nick a friend
     * @param nick
     * @param friend
     * @param type
     * @return true se la inoltra, false altrimenti
     */
    private boolean inoltraAmicizia(String nick, String friend, Settings.RSPType type) throws UserNotExists {
        String request=Settings.RESPONSE.AMICIZIA+" "+ users.getToken(friend)+" "+nick+" "+type;
        SelectionKey k= keys.get(friend);
        if(k==null) return false; //se non è registrata la key non la inoltro (non è online)
        try {
            synchronized (usingK) {
                while (usingK.putIfAbsent(k, k) != null) {
                    usingK.wait();
                }
            }

            try {
                send(k, request + "\n");
            }catch (IOException e){
                    //e.printStackTrace();
                    /*System.out.println("Disconnected");
                    //todo cancellare sfide attive, logout, cancellare token
                    logout(((MyAttachment)k.attachment()).getNick());
                    k.cancel();*/
                    return false;
            }

            synchronized (usingK) {
                usingK.remove(k);
                usingK.notify();
            }
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param tokens
     * @return Restituisce lista amici, classifica o lista richieste
     * @throws UserNotExists se l'utente non esiste
     */
    private String get(String[] tokens) throws UserNotExists {
        Settings.GetType type=Settings.GetType.valueOf(tokens[3]);
        switch(type){
            case AMICI:
                return Settings.GetType.AMICI+" "+ users.getToken(tokens[1])+" "+ users.listaAmici(tokens[1]).toJSONString();
            case CLASSIFICA:
                return Settings.GetType.CLASSIFICA+" "+ users.getToken(tokens[1])+" "+ users.mostraClassifica(tokens[1]).toJSONString();
            case PENDING:
                return Settings.GetType.PENDING+" "+ users.getToken(tokens[1])+" "+ users.listaRichieste(tokens[1]).toJSONString();
            default:
                return null;
        }
    }

    /**
     * Accetta le connessioni in arrivo
     *
     * @param k
     * @throws IOException
     */
    public void accept(SelectionKey k) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) k.channel();
        SocketChannel client = server.accept();
        System.out.println("Accepted connection from " + client);
        client.configureBlocking(false);
        ByteBuffer buffer = ByteBuffer.allocate(Settings.TCPBUFFLEN);
        //todo attach nick e port
        MyAttachment att=new MyAttachment(buffer);
        SelectionKey cK = client.register(selector, SelectionKey.OP_READ, att);

    }

    /**
     * Legge il channell associato a k
     *
     * @param k
     * @throws IOException
     */
    public String read(SelectionKey k) throws IOException {
        //todo scanner
        StringBuilder request = new StringBuilder();
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buffer = ((MyAttachment) k.attachment()).getBuffer();

        //Scanner scanner = new Scanner(client.socket());
        //TODO lasciare nel buffer i comandi dopo \n oppure invio la quantità da leggere
        int read = client.read(buffer);
        byte[] bytes;
        while (read > 0) {

            buffer.flip();
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            request.append(new String(bytes));
            buffer.clear();
            read = client.read(buffer);
        }

        if (read == -1) throw new IOException("Closed");
        buffer.clear();
        return request.toString();
        //return scanner.nextLine();
    }

    /**
     * Scrive sul channell associato a k
     *
     * @param k
     * @param response
     * @throws IOException
     */
    public void send(SelectionKey k, String response) throws IOException {
        //todo send scrittura multiplo di BUFFLEN
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buffer = ((MyAttachment) k.attachment()).getBuffer();
        int written = 0;
        //todo align response to kBUFFLEN
        response.length();
        byte[] b = response.getBytes();
        while ((b.length - written) > 0) { //ciclo fino a che non ho scritto tutto
            if ((b.length - written) % buffer.capacity() != 0)
                buffer.put(Arrays.copyOfRange(b, written, (written) + ((b.length - written) % buffer.capacity())));
            else buffer.put(Arrays.copyOfRange(b, written, (written) + buffer.capacity())); //copio una parte


            buffer.flip();
            int w = 0;
            while (w < buffer.limit()) {
                //System.out.println(new String(buffer.array()));
                w = client.write(buffer);

                written += w;
                //System.out.println(w);
            }

            buffer.clear();
        }

    }

}
