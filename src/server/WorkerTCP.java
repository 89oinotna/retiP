package server;

import exceptions.UserAlreadyLogged;
import exceptions.UserNotExists;
import exceptions.WrongCredException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerTCP implements Runnable {
    private SelectionKey k;
    private int BUFFLEN=1024;
    private Selector selector;
    private Utenti utenti;
    private ConcurrentHashMap<SelectionKey, SelectionKey> usingK;
    public WorkerTCP(Selector _selector, SelectionKey _key, Utenti _utenti, ConcurrentHashMap<SelectionKey, SelectionKey> _usingK) {
        selector=_selector;
        k=_key;
        usingK=_usingK;
        utenti=_utenti;
    }



    public void run() {
        try {
            if (k.isAcceptable()) { //se è il channel sul quale accetto le connessioni
                accept(k);
            } else if (k.isReadable()) {
                String command = read(k);
                System.out.println(command);
                String response = "";//ManagerLogin.login(cred[0], cred[1]);
                response=manageCommand(command, k)+"\n";
                System.out.println(response);
                send(k, response);
                //System.out.println("boh");

            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Disconnected");
            k.cancel();
        }
        finally {
            usingK.remove(k);
        }

    }

    public String manageCommand(String cmd, SelectionKey k){
        //todo mandare la porta udp al login
        String[] tokens=cmd.split(" ");
        switch(tokens[0]){
            case "LOGIN":
                //login nick pw
                return login(tokens, k);
            case "SFIDA":
                //sfida nick token nickAmico
                return sfida(tokens);

            case "AMICIZIA":
                //amicizia nick token nickamico richiedi/accetta/rifiuta
                return amicizia(tokens);

            case "PAROLA":
                //parola nick token parola traduzione
                //return parola();
                break;
            case "GET":
                return get(tokens);

            default:
                break;
        }
        return null;
    }

    private String get(String[] tokens) {
        if(tokens[4].equals("FRIENDS")){
            StringBuilder response= new StringBuilder("OK ");
            Iterator<String> it=utenti.getPending(tokens[1]);
            while(it.hasNext()) response.append(it.next());
            return response.toString();
        }
        return "NOK";
    }

    /**
     * gestisce la richiesta di login
     * @param k selectionkey associata
     * @return token di login se la richiesta è corretta
     */
    public String login(String[] tokens, SelectionKey k){
        //todo al login mostrare le nuove richieste di amicizia in sospeso (o contatore)
        try{
            return "OK "+utenti.loginUtente(tokens[1], tokens[2], k)+" "+utenti.getPendingSize(tokens[1]);

        } catch (UserAlreadyLogged ex) {
            return "NOK UserAlreadyLogged";
        }   catch(WrongCredException ex){
            return "NOK WrongCredException";
        }catch (UserNotExists ex){
            return "NOK UserNotExists";
        }
    }

    /**
     * Crea la sfida tra i due utenti inoltrando la richiesta udp all'altro
     * @param tokens
     * @return true false
     */
    public String sfida(String[] tokens){

        if (utenti.validateToken(tokens[1], tokens[3]) && !utenti.hasSfidaAttiva(tokens[1])){
            //TODO genera worker udp e inoltra la richiesta (utilizzare future?)
            utenti.creaSfida(tokens[1], tokens[2]);
            return "OK";
        }
        else{
            return "NOK";
        }
    }

    /**
     * inoltra la richiesta di amicizia a friend
     * @param nick
     * @param friend
     * @return false se non la riesce a chiedere online
     */
    public boolean richiediAmicizia(String nick, String friend){
        //todo se ce l'ho tra i pending l'accetto direttamente
        if(utenti.exists(friend) && !friend.equals(nick)) {
            if(utenti.getUtente(friend).addPending(nick)) {

                if (utenti.isLogged(friend)) { //se è loggato gliela mando
                    // (lo aggiungo lo stesso alla pending?)
                    SelectionKey kFriend = utenti.getUtente(friend).getK();
                    //AMICIZIA nick token(friend) RICHIESTA
                    String response = "AMICIZIA " + nick + " " + utenti.getUtente(friend).getToken() + " RICHIESTA \n";
                    try {
                        while (utenti.isLogged(friend)) {

                            send(kFriend, response);
                            break;

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;

                } else {

                    return false;
                }
            }
            else{
                utenti.aggiungiAmicizia(nick, friend);
                return true;
            }
        }
        else{
            throw new UserNotExists();
        }
    }

    public boolean rispondiAmicizia(String nick, String friend){

        if(utenti.exists(friend)) {
            utenti.getUtente(friend).addPending(nick);
            if (utenti.isLogged(friend)) { //se è loggato gliela mando
                // (lo aggiungo lo stesso alla pending?)
                SelectionKey kFriend = utenti.getUtente(friend).getK();
                while(true) {
                    if (k.isWritable()) break;
                }
                //AMICIZIA nick token(friend) RICHIESTA
                String response = "AMICIZIA " + nick + " " + utenti.getUtente(friend).getToken() + " RICHIESTA";
                try {
                    send(kFriend, response);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;

            } else {
                //aggiungi alla lista delle richieste pendenti se offline
                //todo se utente non esiste

                return true;
            }
        }
        else{
            throw new UserNotExists();
        }
    }

    public String amicizia(String[] tokens){

        if (utenti.validateToken(tokens[1], tokens[2])){
            switch(tokens[4]){
                case "RICHIESTA":
                    //inoltra la richiesta all'altro
                    richiediAmicizia(tokens[1], tokens[3]);
                    break;
                case "ACCETTA":
                    //rimuovo dalla pending
                    //l'altro ha accettato, crea il legame e inoltra l'amicizia
                    try{
                        utenti.removePendingFriend(tokens[1], tokens[3]);
                        utenti.aggiungiAmicizia(tokens[1], tokens[3]);

                    }catch (UserNotExists ex){
                        return "NOK UserNotExists";
                    }
                    //todo invia richiesta accettata
                    break;
                case "RIFIUTA":
                    //rimuovo dalla pending
                    //l'altro non ha accettato, inoltra il rifiuto
                    //todo gestione getutente null
                    utenti.getUtente(tokens[1]).removePending(tokens[3]);
                    //TODO invia rifiuto
                    break;
            }

            return "OK";
        }
        else{
            return "NOK";
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
        ByteBuffer buffer = ByteBuffer.allocate(BUFFLEN);
        SelectionKey cK = client.register(selector, SelectionKey.OP_READ, buffer);

    }

    /**
     * Legge il channell associato a k
     *
     * @param k
     * @throws IOException
     */
    public String read(SelectionKey k) throws IOException {
        StringBuilder request = new StringBuilder();
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buffer = (ByteBuffer) k.attachment();

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
        ByteBuffer buffer = (ByteBuffer) k.attachment();
        int written = 0;
        //todo align response to kBUFFLEN
        response.length();
        byte[] b = response.getBytes();
        while ((b.length - written) > 0) { //ciclo fino a che non ho scritto tutto
            if ((b.length - written) % BUFFLEN != 0)
                buffer.put(Arrays.copyOfRange(b, written, (written) + ((b.length - written) % BUFFLEN)));
            else buffer.put(Arrays.copyOfRange(b, written, (written) + BUFFLEN)); //copio una parte


            buffer.flip();
            int w = 0;
            while (w < buffer.limit()) {
                //System.out.println(new String(buffer.array()));
                w = client.write(buffer);

                written += w;
                System.out.println(w);
            }

            buffer.clear();
        }

    }

}
