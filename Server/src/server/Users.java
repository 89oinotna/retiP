package server;

import exceptions.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.management.monitor.Monitor;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Users implements IUsers {

    private final ConcurrentHashMap<String, User> users;

    public Users(JSONArray usersJSON) {
        users=new ConcurrentHashMap<>();
        for (Object utenteJSON : usersJSON) {
            User curr = new User((JSONObject) utenteJSON);
            users.put(curr.getNickname(), curr);
        }
    }
    public Users() {
        users=new ConcurrentHashMap<>();
    }

    public void registraUtente(String nick, String pw) throws UserAlreadyExists {
        //String token=generateToken(nick, pw);
        if ((users.putIfAbsent(nick, new User(nick, pw))) != null) throw new UserAlreadyExists();

    }

    public String login(String nick, String pw) throws UserNotExists, WrongCredException, UserAlreadyLogged {
        String token = generateToken(nick, pw);
        if(users.computeIfPresent(nick, (key, oldU) -> {
            if(oldU.getPassword().equals(pw)){  //Controllo la password
                oldU.login(token);
            }
            else{
                throw new WrongCredException();
            }
            return oldU;
        }) != null){ //Se l'utente esiste
            return token;
        }
        else{
            throw new UserNotExists();
        }
    }

    public void logout(String nick) {
        users.computeIfPresent(nick, (key, oldU) -> {
            oldU.logout();
            return oldU;
        });
    }

    public boolean isLogged(String nick){
        return users.get(nick).isLogged();
    }

    public boolean exists(String nick) {
        return users.get(nick)!=null;
    }


    /**
     * Restituisce un iteratore sugli utenti
     */
    public Iterator<User> getIterator() {
        return users.values().iterator();
    }

    /*                      AMICIZIA                        */

    public boolean aggiungiAmico(String nick, String friend) throws UserNotExists {
        if (users.get(friend) == null) throw new UserNotExists();

        synchronized (users) {
            if(hasFriend(nick, friend)) return false;
            //aggiorno la lista di nick
            users.computeIfPresent(nick, (key, oldU) -> {
                oldU.removePending(friend);
                oldU.addFriend(friend);

                return oldU;
            });

            //aggiorno la lista di friend
            users.computeIfPresent(friend, (key, oldU) -> {
                oldU.removePending(nick);
                oldU.addFriend(nick);
                return oldU;
            });
        }
        return true;
    }

    public boolean hasFriend(String nick, String friend) {
        return users.get(nick).hasFriend(friend);
    }

    /**
     * Restituisce la lista degli amici
     * @param nick utente da cui si vuole ottenere la lista amici
     * @return JSONARRAY con la lista degli amici
     */
    public JSONArray listaAmici(String nick) throws UserNotExists {
        JSONArray array=new JSONArray();
        array.addAll(getFriends(nick));
        return array;
    }

    public List<String> getFriends(String nick) throws UserNotExists{
        try {
            return users.get(nick).getFriends();
        }catch (NullPointerException e){
            throw new UserNotExists();
        }
    }

    /**
     * Restituisce la lista delle richieste di amicizia
     * @param nick utente da cui si vuole ottenere la lista delle richieste
     * @return lista delle richieste
     */
    public JSONArray listaRichieste(String nick) {
        JSONArray array=new JSONArray();
        array.addAll(getPendings(nick));
        return array;
    }

    public List<String> getPendings(String nick) {
        return users.get(nick).getPendings();
    }

    public boolean addPending(String nick, String friend) throws FriendshipException {
        return users.get(friend).addPending(nick);
    }

    public void removePending(String nick, String friend){
        users.get(nick).removePending(friend);
    }

    public boolean containsPending(String nick, String friend) {
        return users.get(nick).getPendings().contains(friend);
    }

    /*                      CLASSIFICA                      */

    public List<IUser> getLeaderboard(String nick){
        List<String> amici=users.get(nick).getFriends();
        List<IUser> ut = new ArrayList<>();
        ut.add(users.get(nick));
        for (String s : amici) {
            ut.add(users.get(s));
        }
        ut.sort(Comparator.comparingInt(IUser::getScore).reversed());
        return ut;
    }

    /**
     * Restituisce la classifica per l'utente
     * @param nick utente
     * @return JSONArray contenente la classifica
     */
    public JSONArray mostraClassifica(String nick){
        List<IUser> ut = getLeaderboard(nick);
        JSONArray array=new JSONArray();
        for (IUser u:ut) {
            JSONObject user=new JSONObject();
            user.put("nick", u.getNickname());
            user.put("score", u.getScore());
            array.add(user);
        }
        return array;

    }

    public int mostraPunteggio(String nick) {
        return users.get(nick).getScore();
    }

    /*                      CHALLENGE                       */

    /**
     * Crea la sfida tra i due users e rimuove la richiesta di sfida
     * @param nick utente che accetta la sfida
     * @param friend utente che aveva richiesto la sfida
     * @return la sfida appena creata
     * @throws UserNotOnline        se almeno uno dei due non è online
     * @throws UserAlreadyInGame    se almeno uno dei due è in gioco
     * @throws ChallengeException   se non esiste la richiesta di sfida fatta da friend a nick
     * @throws UserNotExists        se almeno uno dei due non esiste
     */
    public Challenge sfida(String nick, String friend) throws UserAlreadyInGame, UserNotOnline, ChallengeException, UserNotExists {
       Challenge c;
       synchronized (users) {
           if (isLogged(nick) && isLogged(friend)) {
               if (!isInChallenge(nick) && !isInChallenge(friend)) {
                   if(hasChallengeRequest(nick, friend)) {
                        users.get(nick).removeChallengeRequest(friend);
                       //la sfida era stata richiesta da friend
                       c = new Challenge(users.get(nick), users.get(friend));
                       users.get(nick).setChallenge(c);
                       users.get(friend).setChallenge(c);
                   }
                   else throw new ChallengeException();
               } else
                   throw new UserAlreadyInGame();
           } else
               throw new UserNotOnline();
       }
       return c;

    }

    /**
     * Controlla se l'utente nick ha una richiesta di sfida da friend
     * @param nick utente sul quale effettuare il controllo
     * @param friend utente che dovrebbe aver mandato la richiesta
     * @return true se esiste, false altrimenti
     */
    public boolean hasChallengeRequest(String nick, String friend) {
        return users.get(nick).hasChallengeRequest(friend);
    }

    /**
     * Restituisce l'oggetto sfida associato a nick
     * @param nick utente
     * @return sfida
     */
    public Challenge getChallenge(String nick){
        return users.get(nick).getChallenge();
    }

    /**
     * Controlla se l'utente ha una sfida attiva
     * @param nick utente
     * @return true se ha una sfida attiva, false altrimenti
     * @throws UserNotExists se l'utente nick non esiste
     */
    public boolean isInChallenge(String nick) throws UserNotExists{
        User u=users.get(nick);
        if(u==null) throw new UserNotExists();
        synchronized (u) {
            return u.isInChallenge();
        }
    }

    /**
     * Aggiunge la richiesta di sfida fatta da nick
     * a friend alle richieste di sfida ricevute di friend
     * @param nick from
     * @param friend to
     * @return true se non era presente già
     */
    public boolean addPendingChallenge(String nick, String friend) throws UserNotExists {
        try {
            synchronized(users){
                return users.get(friend).addChallengeRequest(nick);
            }

        }catch (NullPointerException e){
            throw new UserNotExists();
        }
    }

    /**
     * Rimuove la richiesta di sfida fatta da nick a friend
     * @param nick from
     * @param friend to
     * @return true se era presente ed è stato rimosso, false altrimenti
     * @throws UserNotExists se l'utente non esiste
     */
    public  boolean  removePendingChallenge(String nick, String friend) throws UserNotExists {
        try {
            synchronized(users){
                return users.get(friend).removeChallengeRequest(nick);
            }
        }catch (NullPointerException e){
            throw new UserNotExists();
        }
    }

    /*                      TOKEN                       */

    /**
     * Restituisce il token associato all'utente
     * @param nick
     * @return token
     * @throws UserNotExists se l'utente non esiste
     */
    public String getToken(String nick)throws UserNotExists{
        try {
            return Objects.requireNonNull(users.get(nick)).getToken();
        }catch (NullPointerException e){
            throw new UserNotExists();
        }
    }

    /**
     * Genera il token per l'utente
     * @param nick
     * @param pw
     * @return token generato
     */
    private String generateToken(String nick, String pw){
        String token="";
        try {
            MessageDigest md5=MessageDigest.getInstance("MD5");
            md5.update((nick+pw+LocalDateTime.now()).getBytes());
            byte[] digest = md5.digest();
            BigInteger no = new BigInteger(1, digest);
            // Convert message digest into hex value
            token = no.toString(16);
            while (token.length() < 32) {
                token = "0" + token;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return token;
    }

    /**
     * Controlla se il token corrisponde a quello di user
     * @param nick
     * @param token
     * @return true se corrisponde false altrimenti
     * @throws UserNotExists se l'utente non esiste
     */
    public boolean validateToken(String nick, String token) throws UserNotExists{
            return getToken(nick).equals(token);
    }


}
