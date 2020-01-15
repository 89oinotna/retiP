package server;

import exceptions.FriendshipException;
import exceptions.UserAlreadyLogged;
import exceptions.UserNotExists;
import exceptions.WrongCredException;

import Settings.Settings;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

//todo wait for reply (cosi non si accodano le risposte)
public class WorkerTCP implements Runnable {
    private SelectionKey k;
    private int BUFFLEN=1024;
    private Selector selector;
    private Users users;
    private final ConcurrentHashMap<SelectionKey, SelectionKey> usingK;
    private ConcurrentHashMap<String, SelectionKey> keys;

    public WorkerTCP(Selector s, SelectionKey k, Users users, ConcurrentHashMap<SelectionKey, SelectionKey> usingK, ConcurrentHashMap<String, SelectionKey> keys) {
        selector=s;
        this.k=k;
        this.usingK = usingK;
        this.users = users;
        this.keys=keys;
    }

    public void run() {
        try {
            if (k.isAcceptable()) { //se è il channel sul quale accetto le connessioni
                accept(k);
            } else if (k.isReadable()) {
                String command = read(k);
                System.out.println(command);
                String response = null;
                try {
                    response = manageCommand(command, k);

                }catch(Exception e){
                    response=e.toString().split(":")[0];
                }
                finally{
                    if(response!=null) {
                        System.out.println("Response: "+ response);
                        send(k, response+ "\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Disconnected");
            //todo cancellare sfide attive, logout, cancellare token
            k.cancel();
        }
        finally {
            synchronized (usingK) {
                usingK.remove(k);
            }
        }

    }

    public String manageCommand(String cmd, SelectionKey k) throws FriendshipException, UserNotExists, WrongCredException, UserAlreadyLogged {
        //todo mandare la porta udp al login
        String[] tokens=cmd.split(" ");
        Settings.REQUEST r = Settings.REQUEST.valueOf(tokens[0]);
        switch(r){
            case LOGIN:
                return login(tokens, k);
            case SFIDA:
                //todo update della classifica per tutti i client amici
                return sfida(tokens);
            case AMICIZIA:
                return amicizia(tokens);
            case GET:
                return get(tokens);

            /*case "PAROLA":
                //parola nick token parola
                //return parola();
                break;*/
        }

        return null;
    }


    /**
     * Gestisce la richiesta di login
     * @param k selectionkey associata
     * @param tokens request string tokenizzata
     * @return
     */
    public String login(String[] tokens, SelectionKey k)throws WrongCredException, UserNotExists, UserAlreadyLogged{
        String token=users.login(tokens[1], tokens[2]);
        keys.put(tokens[1], k);
        return  Settings.RESPONSE.OK+" "+ token+" \n"+
                Settings.RESPONSE.AMICI+" "+ token+" "+ users.listaAmici(tokens[1]).toJSONString()+" \n"+
                Settings.RESPONSE.PENDING+" "+ token+" "+ users.listaRichieste(tokens[1]).toJSONString()+" \n" +
                Settings.RESPONSE.CLASSIFICA+" "+ token+" "+ users.mostraClassifica(tokens[1]).toJSONString();

    }

    /**
     * Crea la sfida tra i due users inoltrando la richiesta udp all'altro
     * @param tokens
     * @return true false
     */
    public String sfida(String[] tokens){
        /*InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String msg="SFIDA "+i+" "+ System.currentTimeMillis();
        byte[] sendData=msg.getBytes();
        DatagramPacket sPacket=new DatagramPacket(sendData, sendData.length, IPAddress, port);
        cs.send(sPacket);
        if (users.validateToken(tokens[1], tokens[2]) ){

            try{
                users.sfida(tokens[1], tokens[3]);}
            catch(UserNotOnline e){
                return "NOK UserNotOnline";
            }
            catch(UserAlreadyInGame e){
                return "NOK UserAlreadyInGame";
            }
            return "OK";
        }
        else{
            return "NOK";
        }*/
        return null;
    }

    public String amicizia(String[] tokens) throws FriendshipException, WrongCredException, UserNotExists {
        if (users.validateToken(tokens[1], tokens[2])){
            Settings.RQTType type= Settings.RQTType.valueOf(tokens[4]);
            switch(type){
                case RICHIEDI:
                    richiediAmicizia(tokens[1], tokens[3]);
                    break;
                case ACCETTA:
                    accettaAmicizia(tokens[1], tokens[3]);
                    break;
                case RIFIUTA:
                    //rimuovo dalla pending
                    users.removePending(tokens[1], tokens[3]);
                    break;
            }

            return "OK";
        }
        else{
            throw new WrongCredException();
        }
    }


    /**
     * inoltra la richiesta di amicizia a friend
     * @param nick
     * @param friend
     * @return false se non la riesce a chiedere online
     */
    public boolean richiediAmicizia(String nick, String friend) throws FriendshipException, UserNotExists {
        //todo
        //todo inoltra la richiesta all'altro
        if(users.exists(friend) && !friend.equals(nick)) {
            if(users.addPending(friend, nick)) { //aggiungo nick ai pending di friend
                inoltraAmicizia(nick, friend, Settings.RSPType.RICHIESTA);
                return true;
            }
            else{   //se invece era già presente accetto l'amicizia direttamente
                accettaAmicizia(nick, friend);
                return true;
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
     */
    private void inoltraAmicizia(String nick, String friend, Settings.RSPType type) throws UserNotExists {
        String request=Settings.REQUEST.AMICIZIA+" "+ users.getToken(friend)+" "+nick+" "+type;
        SelectionKey k= keys.get(friend);
        if(k==null) return; //se non è registrata la key non la inoltro (non è online)
        try {
            synchronized (usingK) {
                while (usingK.putIfAbsent(k, k) != null) {
                    usingK.wait();
                }
            }
            try {
                send(k, request + "\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (usingK) {
                usingK.remove(k);
                usingK.notify();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private String get(String[] tokens) throws UserNotExists {
        switch(tokens[3]){
            case "AMICI":
                break;
            case "CLASSIFICA":
                return "CLASSIFICA "+ users.getToken(tokens[1])+" "+ users.mostraClassifica(tokens[1]).toJSONString();

            case "PENDING":
                break;
            default:
                return "NOK";
        }
        return "";
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
        //todo scanner
        StringBuilder request = new StringBuilder();
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buffer = (ByteBuffer) k.attachment();


        //Scanner scanner = new Scanner(client.socket());
        //TODO lasciare nel buffer i comandi dopo \n
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
                //System.out.println(w);
            }

            buffer.clear();
        }

    }

}
