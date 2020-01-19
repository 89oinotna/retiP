package server;


import Settings.Settings;
import exceptions.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//todo serve serializable per rmi?
public class Users implements Serializable, IUsers {
    private ConcurrentHashMap<String, User> users;
    private List<String> dict; //concurrent //todo mi serve in users?
    /**
     * prende k parole random dal dizionario
     * @return
     */
    private List<String> getParole() {
        List<String> parole=new ArrayList<>(Settings.k);
        for(int i=0; i<Settings.k; i++){
            String parola=dict.get((int) (Math.random()*(dict.size()-1)));
            while(parole.contains(parola)){
                parola=dict.get((int) (Math.random()*(dict.size()-1)));
            }
            parole.add(parola);
        }
        return parole;
    }

    public Users(List<String> dict) {
        users = new ConcurrentHashMap<>();
        this.dict=dict;
    }

    public Users(JSONArray usersJSON, List<String> dict) {
        users = new ConcurrentHashMap<>();
        for (Object utenteJSON : usersJSON) {
            User curr = new User((JSONObject) utenteJSON);
            users.put(curr.getNickname(), curr);
        }
        this.dict=dict;

    }

    public void registraUtente(String nick, String pw) throws UserAlreadyExists {
        //String token=generateToken(nick, pw);
        if ((users.putIfAbsent(nick, new User(nick, pw))) != null) throw new UserAlreadyExists();

    }

    public String login(String nick, String pw) throws UserNotExists, WrongCredException, UserAlreadyLogged {
        //todo loggo o no se sono loggato già
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

    /*                      AMICIZIA                        */

    public void aggiungiAmico(String nick, String friend) throws UserNotExists {
        if (users.get(friend) == null) throw new UserNotExists();

        synchronized (users) {
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
    }

    public JSONArray listaAmici(String nick) {
        List<String> friends=users.get(nick).getFriends();
        JSONArray array=new JSONArray();
        array.addAll(friends);
        return array;
    }

    public JSONArray listaRichieste(String nick) {
        List<String> pendings=users.get(nick).getPendings();
        JSONArray array=new JSONArray();
        array.addAll(pendings);
        return array;
    }

    public boolean addPending(String nick, String friend) throws FriendshipException {
        return users.get(friend).addPending(nick);
    }

    public void removePending(String nick, String friend){
        users.get(nick).removePending(friend);
    }

    /*                      CLASSIFICA                      */

    public JSONArray mostraClassifica(String nick){
        List<String> amici=users.get(nick).getFriends();
        JSONArray array=new JSONArray();

        List<User> ut = new ArrayList<User>();
        ut.add(users.get(nick));
        for (String s : amici) {
            ut.add(users.get(s));
        }
        ut.sort(Comparator.comparingInt(User::getScore).reversed());

        for (User u:ut) {
            JSONObject user=new JSONObject();
            user.put("nick", u.getNickname());
            user.put("score", u.getScore());
            array.add(user);
        }
        //System.out.println("array:"+array.toJSONString());
        return array;

    }

    public int mostraPunteggio(String nick) {
        return users.get(nick).getScore();
    }

    /*                      CHALLENGE                       */

    public Challenge sfida(String nick, String friend) throws UserAlreadyInGame, UserNotOnline, ChallengeException {
        //todo se uno si disconnette quello che rimane continua da solo
        //TODO genera pacchetto udp e inoltra la richiesta (utilizzare future?)
       Challenge c;
       synchronized (users) {
           if (isLogged(nick) && isLogged(friend)) {
               if (!isInChallenge(nick) && !isInChallenge(friend)) {
                   if(hasChallengeRequest(nick, friend)) {
                        users.get(nick).removeChallengeRequest(friend);
                       //la sfida era stata richiesta da friend
                       c = new Challenge(users.get(nick), users.get(friend),  getParole());
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

    public boolean hasChallengeRequest(String nick, String friend) {
        return users.get(nick).hasChallengeRequest(friend);
    }

    public Challenge getChallenge(String nick){
        return users.get(nick).getChallenge();
    }

    public boolean isInChallenge(String nick){
        Challenge c=users.get(nick).getChallenge();
        if(c==null) return false;
        synchronized (c) {
            return c.isActive();
        }
    }

    public int aggiornaPunteggio(String nick, int punteggio)throws UserNotExists {
        try {
            return Objects.requireNonNull(users.computeIfPresent(nick, (key, oldU) -> {
                oldU.addScore(punteggio);
                return oldU;
            })).getScore();
        }catch (NullPointerException e){
            throw new UserNotExists();
        }
    }




    /*public User getUtente(String nick){
        return users.get(nick);
    }*/


    public boolean isLogged(String nick){
        return users.get(nick).isLogged();
    }

    public boolean exists(String nick) {
        return users.get(nick)!=null;
    }

    public Iterator<User> getIterator() {
        return users.values().iterator();
    }

    /*                      TOKEN                       */

    /**
     * Restituisce il token associato all'utente
     * @param nick
     * @return
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
     * @return token
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

    public boolean containsPending(String nick, String friend) {
        return users.get(nick).getPendings().contains(friend);
    }

    //todo termina sfida

    /**
     * Termina la sfida e aggiorna i punteggi
     * @param c
     * @return true se la termina flase altrimenti
     * @throws UserNotExists
     */
    public boolean terminaSfida(Challenge c) throws UserNotExists {
        synchronized (c) {

            if(c.endChallenge()){

                List<String> users=c.getUsers();
                aggiornaPunteggio(users.get(0), c.getScore(users.get(0)));
                aggiornaPunteggio(users.get(1), c.getScore(users.get(1)));
                return true;
            }

        }
        return false;
    }

    /**
     * Aggiunge la richiesta di sfida fatta da nick
     * a friend alle richieste di sfida ricevute di friend
     * @param nick from
     * @param friend to
     * @returns true se non era presente già
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
     *
     * @param nick from
     * @param friend to
     * @return true se è stato rimosso
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

    public boolean hasFriend(String nick, String friend) {
        return users.get(nick).hasFriend(friend);
    }

    public List<String> getListaAmici(String nick) {
        return users.get(nick).getFriends();
    }
}
