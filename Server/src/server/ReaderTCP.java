package server;

import exceptions.*;

import Settings.Settings;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Worker che si occupa della lettura di tutti i comandi TCP
 * e della creazione della risposta
 *
 * Le operazioni di inoltro vengono effettuate da un nuovo thread (Notifier)
 *
 * Durante le operazioni del reader k.interestops()=0
 */
public class ReaderTCP implements Runnable {

    private SelectionKey k;
    private final Users users;
    private UDP udp;
    private ConcurrentHashMap<String, SelectionKey> keys;  //contiene le key di tutti gli utenti loggati
    private String token;
    private ExecutorService executor;  //threadpool a cui aggiungo anche i notifier
    public ReaderTCP(SelectionKey k, Users users,
                     ConcurrentHashMap<String, SelectionKey> keys, UDP udp, ExecutorService executor) {
        this.executor=executor;
        this.udp=udp;
        this.k=k;
        this.users = users;
        this.keys=keys;
        token=null;
    }

    public void run() {
        try {
            String command = read();
            if(command.length()>0) {
                SocketChannel client = (SocketChannel) k.channel();

                System.out.println("FROM: " + ((SocketChannel) k.channel()).getRemoteAddress());
                System.out.print("REQUEST: " + command);
                String response = null;
                try {
                    response = manageCommand(command);

                } catch (CustomException e ) {
                    e.printStackTrace();
                    response = Settings.RESPONSE.NOK +" " + e.toString().split(":")[0]+ " "+((MyAttachment)k.attachment()).getToken();
                } finally {
                    synchronized (k) {
                        if (response != null) {
                                ((MyAttachment)k.attachment()).setResponse(response);
                                k.interestOps(SelectionKey.OP_WRITE);
                        }
                        else
                            k.interestOps(SelectionKey.OP_READ);
                        k.notify();
                        k.selector().wakeup();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            String nick=((MyAttachment)k.attachment()).getNick();
            if(nick!=null) {
                System.out.println("Disconnected " +nick);
                logout(nick);
            }
            keys.remove(k);
            synchronized (k) {
                k.cancel();
                k.notify();
            }
        }

    }

    /**
     * Funzione principale che effettua il parse del comando
     *  e invoca la funzione per gestirlo
     * @param cmd comando ricevuto
     * @return risposta per il comando ricevuto
     */
    public String manageCommand(String cmd) throws FriendshipException, UserNotExists, WrongCredException, UserAlreadyLogged, UserNotOnline, UserAlreadyInGame, ChallengeException {
        String[] tokens=cmd.split(" ");
        //token=tokens[2];
        Settings.REQUEST r = Settings.REQUEST.valueOf(tokens[0]);
        if(r.equals(Settings.REQUEST.LOGIN)) return Settings.RESPONSE.OK + " " + login(tokens[1], tokens[2], tokens[3]);
        else if(((MyAttachment)k.attachment()).getToken().equals(tokens[2])) {
            token=tokens[2];
            switch (r) {
                case LOGOUT:
                    logout(tokens[1]);
                    return null;
                case SFIDA:
                    return sfida(tokens[1], tokens[3], tokens[4]);
                case AMICIZIA:
                    return Settings.RESPONSE.OK + " " + amicizia(tokens[1], tokens[3], tokens[4]);
                case GET:
                    return get(tokens[1], tokens[3]);
                case PAROLA:
                    return Settings.RESPONSE.OK + " " + parola(tokens[1], tokens[3]);
            }
            throw new IllegalArgumentException();
        }else throw new WrongCredException();
    }

    /**
     * Gestisce il comando di login
     * @param nick nickname utente che effettua il login
     * @param pw password utente
     * @param p porta udp dell'utente su cui inoltrare le richieste di sfida
     * @return stringa per la risposta conntenente le informazioni di login (amici, richieste, classifica)
     * @throws WrongCredException se i dati di login non sono corretti
     * @throws UserNotExists se l'utente non esiste
     * @throws UserAlreadyLogged se l'utente è già loggato
     */
    public String login(String nick, String pw, String p)throws WrongCredException, UserNotExists, UserAlreadyLogged{
        //todo possibilità di riconnessione alla sfida dopo disconnessione
        String token=users.login(nick, pw);
        Integer port=Integer.valueOf(p);    //la porta UDP viene passata come parametro al comando di login
        keys.put(nick, k);
        ((MyAttachment)k.attachment()).setNick(nick).setUDPPort(port).setToken(token);  //setto tutte le informazioni nell'attachment
        return  Settings.RESPONSE.LOGIN+" "+token+" \n"+
                Settings.RESPONSE.AMICI+" "+ token+" "+ users.listaAmici(nick).toJSONString()+" \n"+
                Settings.RESPONSE.PENDING+" "+ token+" "+ users.listaRichieste(nick).toJSONString()+" \n" +
                Settings.RESPONSE.CLASSIFICA+" "+ token+" "+ users.mostraClassifica(nick).toJSONString();

    }

    /**
     * Gestisce il comando di logout
     * @param nick request string tokenizzata
     */
    public void logout(String nick) {
        users.logout(nick);
        keys.remove(((MyAttachment)k.attachment()).getNick());
        k.cancel();
    }

    /*                      SFIDA                       */

    /**
     * Crea la sfida tra i due users inoltrando la richiesta udp all'altro
     * @param nick utente che invia il comando
     * @param friend amico
     * @param type tipo di richiesta sulla sfida
     * @return risposta per il comando di sfida
     * @throws UserNotOnline se uno dei due non è online
     * @throws UserNotExists se uno dei due non esiste
     * @throws UserAlreadyInGame se uno dei due ha una sfida attiva
     * @throws ChallengeException se richiedo la sfida a qualcuno a cui l'ho già chiesta,
     *                            se richiedo la sfida a qualcuno che me l'ha già richiesta
     *                            oppure voglio accettare la sfida ma non esiste la richiesta
     */
    public String sfida(String nick, String friend, String type) throws UserNotOnline, UserNotExists, UserAlreadyInGame, ChallengeException {
        switch (Settings.RQTType.valueOf(type)) {
            case RICHIEDI:
                return richiediSfida(nick, friend);
            case ACCETTA:
                return accettaSfida(nick, friend);
            case RIFIUTA:
                return rifiutaSfida(nick, friend);
        }
        throw  new IllegalArgumentException();
    }

    /**
     * Richiede la sfida a friend inoltrandola su UDP creando un timer per gestire il timeout della richiesta
     * @param nick from
     * @param friend to
     * @return stringa che mi dice che la richiesta è stata richiesta
     * @throws UserNotOnline se friend non è online
     * @throws UserNotExists se uno dei due non esiste
     * @throws ChallengeException se richiedo la sfida a qualcuno a cui l'ho già chiesta,
     *                            se richiedo la sfida a qualcuno che me l'ha già richiesta
     */
    public String richiediSfida(String nick, String friend) throws UserNotOnline, UserNotExists, ChallengeException {
        try {
            if (!users.isInChallenge(nick) && !users.isInChallenge(friend)
                    && !users.hasChallengeRequest(nick, friend)
                    && users.addPendingChallenge(nick, friend)) {

                if (inoltraRichiestaSfida(nick, friend)) {
                    //avvio il timer per il timeout della richiesta
                    Timer t = new Timer();
                    TimerTask tt = new TimerChallengeRequest(nick, friend, keys.get(nick), keys.get(friend), users, executor);
                    t.schedule(tt, Settings.TIMEOUT);
                    return Settings.RESPONSE.OK + " " + Settings.RESPONSE.SFIDA + " " + token + " " + friend + " " + Settings.SFIDA.RICHIESTA;
                } else {   //se non la inoltra cancello la richiesta
                    users.removePendingChallenge(nick, friend);
                    throw new UserNotOnline();
                }
            }
            else //portei accettarla quando richiedo la sfida a qualcuno che me l'ha già chiesta
                throw new ChallengeException();

        } catch (UserNotExists | UserNotOnline e ){ //cancello la richiesta
            users.removePendingChallenge(nick, friend);
            throw e;
        }
    }

    /**
     * Accetta la sfida e la crea per i due utenti inoltrando l'accettazione a chi l'aveva richiesta
     * @param nick utente che accetta la sfida
     * @param friend utente che aveva inviato la richiesta di sfida
     * @return stringa di sfida accettata
     * @throws ChallengeException se non esiste la richiesta di sfida da friend a nick
     * @throws UserNotOnline se almeno uno dei due utenti non è online
     * @throws UserNotExists se non esiste
     * @throws UserAlreadyInGame se almeno uno è in gioco
     */
    public  String accettaSfida(String nick, String friend) throws ChallengeException, UserNotOnline, UserNotExists, UserAlreadyInGame {
        Challenge c = users.sfida(nick, friend);
        c.setKeys(k, keys.get(friend)).setExecutor(executor);
        //schedulo il timer di chiusura sfida
        Timer t = new Timer();
        TimerTask tt = new TimerChallengeEnd(keys.get(nick), keys.get(friend), c, users, keys, executor);
        t.schedule(tt, Settings.timer);
        try{
            inoltraSfida(nick, friend, Settings.SFIDA.ACCETTATA);

        }catch (UserNotOnline e){ //se non riesco a inoltrare termino la sfida
            c.endChallenge();
            throw e;
        }
        return Settings.RESPONSE.OK+" "+Settings.RESPONSE.SFIDA + " " + token + " " + friend + " " + Settings.SFIDA.ACCETTATA;

    }

    /**
     * Rifiuta una richiesta di sfida rimuovendola dalle richieste
     * @param nick utente che rifiuta
     * @param friend utente che aveva richiesto la sfida
     * @return stringa di sfida rifiutata
     * @throws UserNotExists se non esiste
     */
    public String rifiutaSfida(String nick, String friend) throws UserNotExists {
        //from friend to me
        users.removePendingChallenge(friend, nick);
        //possibile implementazione per notificare il rifiuto
        /*try{
            inoltraSfida(nick, friend, Settings.SFIDA.RIFIUTATA);
        }catch (UserNotOnline ignored){
            //significa che non è online (ignoro)
        }*/
        return Settings.RESPONSE.SFIDA + " " + token + " " + friend + " " + Settings.SFIDA.RIFIUTATA;

    }

    /**
     * Inoltra la sfida su udp a friend
     * @throws UserNotExists se l'utente non esiste
     * @throws UserNotOnline se l'utente non è online
     * @return true se la inoltra, false se non riesce
     */
    public boolean inoltraRichiestaSfida(String nick, String friend) throws UserNotExists, UserNotOnline {
        if(!users.isLogged(friend)) throw new UserNotOnline();
        try {
            udp.write(nick, friend, users.getToken(friend), ((MyAttachment) keys.get(friend).attachment()).getUDPPort());
            return true;
        }catch (IOException e){
            return false;
        }
    }

    /**
     * Inoltra informazioni sulla sfida a friend
     * @param nick che effettua l'inoltro
     * @param friend che riceve l'inoltro
     * @param type  tipo di inoltro da effettuare per la sfida
     * @param punteggio punteggio da inoltrare per sfida terminata
     * @throws UserNotExists se l'utente a cui inoltrare non esiste
     * @throws UserNotOnline se l'utente a cui inoltrare non è online
     */
    public void inoltraSfida(String nick, String friend, Settings.SFIDA type, int... punteggio) throws UserNotExists, UserNotOnline {
        String request;
        if(type.equals(Settings.SFIDA.TERMINATA))
            request=Settings.RESPONSE.SFIDA+" "+ users.getToken(friend)+" "+nick+" "+type+" "+punteggio[0];
        else{
            request=Settings.RESPONSE.SFIDA+" "+ users.getToken(friend)+" "+nick+" "+type;
        }
        SelectionKey k= keys.get(friend);
        if(k==null) return ; //se non è registrata la key non la inoltro (non è online)
        executor.submit(new Notifier(k, request));



    }

    /**
     * Aggiorna le classifiche degli amici comporeso la propria
     * @param nick utente della sfida
     * @param friend utente della sfida
     * @throws UserNotExists se non esiste uno dei due utenti
     */
    public void aggiornaClassifiche(String nick, String friend) throws UserNotExists {
        List<String> friends=new ArrayList<>();
        friends.addAll(users.getFriends(nick));
        friends.addAll(users.getFriends(friend));
        for (String f:friends) {
            String response;
            try {
                response = Settings.GetType.CLASSIFICA+" "+ users.getToken(f)+" "+ users.mostraClassifica(f).toJSONString();
                SelectionKey k= keys.get(f);
                if(k==null) continue ;
                String finalResponse = response;
                executor.submit(new Notifier(k, finalResponse));
            } catch (UserNotExists userNotExists) {
                userNotExists.printStackTrace();
            }
        }


    }

    /*                      PAROLA                      */

    /**
     * Gestisce l'ivio di una parola della sfida
     * @param nick utente che invia la parola
     * @param parola parola inviata
     * @return risposta con la prossima parola se la sfida non è terminata oppure
     *         risposta di fine sfida con punteggio se la sfida è terminata
     * @throws UserNotExists se l'utente non esiste
     * @throws ChallengeException se l'utente non ha una sfida
     */
    public String parola(String nick, String parola) throws UserNotExists, ChallengeException {
        Challenge c=users.getChallenge(nick);
        if(c==null) throw new ChallengeException();
        synchronized (c) {
            String nextWord = c.tryWord(nick, parola);
            if (nextWord != null) {
                return Settings.RESPONSE.PAROLA + " " + token + " " + nextWord;
            } else { //sfida terminata
                c.endChallenge();
                String friend = c.getOpponent(nick);
                aggiornaClassifiche(nick, friend);
                try {
                    inoltraSfida(nick, friend, Settings.SFIDA.TERMINATA, c.getScore(friend));
                } catch (UserNotOnline ignore) {
                    //ignoro
                }
                return Settings.RESPONSE.SFIDA + " " + token + " " + friend + " " + Settings.SFIDA.TERMINATA + " " + c.getScore(nick);
            }
        }
    }

    /*                      AMICIZIA                    */

    /**
     * Gestisce il comando di amicizia
     * @param nick utente che invia il comando
     * @param friend utente a cui è rivolto
     * @param t tipo di comando sull'amicizia
     * @return risposta per il comando sulla sfida
     * @throws FriendshipException se il comando è di richiesta e gli utenti sono già amici
     * @throws UserNotExists se uno dei due non esiste
     */
    public String amicizia(String nick, String friend, String t) throws FriendshipException, UserNotExists {
        Settings.RQTType type= Settings.RQTType.valueOf(t);
        String response=Settings.RESPONSE.AMICIZIA+" "+token+" "+friend+" ";
        switch(type){
            case RICHIEDI:
                response += Settings.AMICIZIA.RICHIESTA;
                if(!richiediAmicizia(nick, friend)) {
                    response+=" \n"+Settings.RESPONSE.AMICIZIA+" "+token+" "+friend+" "+Settings.AMICIZIA.ACCETTATA;
                }
                /*else{
                    response+=Settings.AMICIZIA.ACCETTATA;
                }*/
                break;
            case ACCETTA:
                //notifico anche la nuova classifica
                accettaAmicizia(nick, friend);
                response+=Settings.AMICIZIA.ACCETTATA+" \n"+
                        Settings.RESPONSE.CLASSIFICA+" "+ token+" "+ users.mostraClassifica(nick).toJSONString();
                break;
            case RIFIUTA:
                //rimuovo dalla pending
                users.removePending(nick, friend);
                response+=Settings.AMICIZIA.RIFIUTATA;
                break;
            default:
                break;
        }
        return response;
    }

    /**
     * Inoltra la richiesta di amicizia a friend aggiungendo
     * Se nick ha già la richiesta di amicizia da friend allora viene accettata direttamente
     * @param nick from
     * @param friend to
     * @return false se accetta direttamente l'amicizia perchè già nei pending
     * @throws FriendshipException se sono già amici
     * @throws UserNotExists se almeno uno dei due non esiste
     */
    public boolean richiediAmicizia(String nick, String friend) throws FriendshipException, UserNotExists {
        if(users.exists(nick) && users.exists(friend) && !friend.equals(nick)) {
            synchronized (users) {
                if(!users.hasFriend(nick, friend)){
                    if (!users.containsPending(nick, friend) && users.addPending(nick, friend)) { //aggiungo nick ai pending di friend
                        inoltraAmicizia(nick, friend, Settings.AMICIZIA.RICHIESTA);
                        return true;
                    } else {   //se invece era già presente accetto l'amicizia direttamente
                        accettaAmicizia(nick, friend);
                        return false;
                    }
                }
                throw new FriendshipException("GIA AMICI");
            }
        }
        else{
            throw new UserNotExists();
        }
    }

    /**
     * Accetta l'amicizia e la inoltra
     * @param nick utente che accetta
     * @param friend utente che ha inviato l'amicizia
     * @throws UserNotExists se non esiste
     * @throws FriendshipException se non era stata richiesta l'amicizia
     */
    public void accettaAmicizia(String nick, String friend) throws UserNotExists, FriendshipException {
        //l'altro ha accettato, crea il legame e inoltra l'amicizia
        if(users.containsPending(nick, friend)) { //controllo che esista la richiesta
            if(users.aggiungiAmico(nick, friend)) {
                inoltraAmicizia(nick, friend, Settings.AMICIZIA.ACCETTATA);
            }
        }
        else{
            throw new FriendshipException("AMICIZIA NON RICHIESTA");
        }
    }

    /**
     * inoltra amicizia da nick a friend
     * @param nick from
     * @param friend to
     * @param type tipo di inoltro
     * @return true se la inoltra, false altrimenti
     */
    public void inoltraAmicizia(String nick, String friend, Settings.AMICIZIA type) throws UserNotExists {
        String request=Settings.RESPONSE.AMICIZIA+" "+ users.getToken(friend)+" "+nick+" "+type;
        SelectionKey k= keys.get(friend);
        if(k==null) return; //se non è registrata la key non la inoltro (non è online)
        executor.submit(new Notifier(k, request));
    }

    /*                      GET                         */

    /**
     * Gestisce i comandi GET
     * @param nick utente che effettua la richiesta
     * @param t tipo di richiesta
     * @return Restituisce JSON con lista amici, classifica o lista richieste
     * @throws UserNotExists se l'utente non esiste
     */
    public String get(String nick, String t) throws UserNotExists {
        Settings.GetType type=Settings.GetType.valueOf(t);
        switch(type){
            case AMICI:
                return Settings.GetType.AMICI+" "+ token+" "+ users.listaAmici(nick).toJSONString();
            case CLASSIFICA:
                return Settings.GetType.CLASSIFICA+" "+ token+" "+ users.mostraClassifica(nick).toJSONString();
            case PENDING:
                return Settings.GetType.PENDING+" "+ token+" "+ users.listaRichieste(nick).toJSONString();
            default:
                return null;
        }
    }

    /*                      READ/SEND                   */

    /**
     * Legge il channell associato a k

     * @throws IOException
     */
    public String read() throws IOException {
        StringBuilder request = new StringBuilder();
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buffer = ((MyAttachment) k.attachment()).getBuffer();
        //TODO lasciare nel buffer i comandi dopo \n oppure invio la quantità da leggere
        //todo leggere solo lunghezza buffer (client deve inviare sempre la stessa lunghezza di comandi)
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





}
