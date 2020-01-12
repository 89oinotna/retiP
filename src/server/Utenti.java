package server;


import exceptions.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Utenti implements Serializable {
    private ConcurrentHashMap<String, Utente> utenti;
    private List<String> dict; //concurrent
    private static final int k = 10; //ms
    private ServerUdp udp;
    public Utenti(List<String> dict) {
        utenti = new ConcurrentHashMap<>();
        this.dict=dict;


    }

    public Utenti(JSONArray utentiJSON, List<String> dict) {
        utenti = new ConcurrentHashMap<>();
        for (Object utenteJSON : utentiJSON) {
            //todo threadpool
            Utente curr = new Utente((JSONObject) utenteJSON);
            utenti.put(curr.getNickname(), curr);
        }
        this.dict=dict;

    }

    public String registraUtente(String nick, String pw) throws UserAlreadyExists {
        String token=generateToken(nick, pw);
        if ((utenti.putIfAbsent(nick, new Utente(nick, pw, token))) != null) throw new UserAlreadyExists();
        else {
            return token;
        }
    }

    public String generateToken(String nick, String pw){
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

    public boolean validateToken(String nick, String token){
        Utente u=utenti.get(nick);
        if(u!=null){
            System.out.println(u.getToken());
            return u.getToken().equals(token);

        }
        else throw new UserNotExists();
    }

    public String loginUtente(String nick, String pw, SelectionKey k) {
        //Utente u = utenti.get(nick);
        //System.out.println("p:"+pw+".");
        final String[] token = new String[1];


            if(utenti.computeIfPresent(nick, (key, oldU) -> {
                if(oldU.getPassword().equals(pw)){
                    if (oldU.getK()==null||!oldU.getK().isValid()||!oldU.isLogged()) {
                        oldU.setK(k);
                        token[0] =generateToken(nick, pw);
                        oldU.logIn(token[0]);
                    }
                    else {
                        throw new UserAlreadyLogged();
                    }
                }
                else{
                    throw new WrongCredException();
                }
                return oldU;
            }) != null){
                return token[0];
            }
            else{
                throw new UserNotExists();
            }



    }

    public boolean logoutUtente(String nick) {
        utenti.computeIfPresent(nick, (key, oldU) -> {
            if (oldU.isLogged()) oldU.logOut();
           /* else {
                throw new UserAlreadyOut();
            }*/
            return oldU;
        });
        return true;
    }

    /**
     * Aggiunge l'amicizia tra nick e friend
     * aggiornando la lista di amici di entrambi
     * se già c'è non fa nulla
     *
     * @param nick
     * @param friend
     */
    public void aggiungiAmicizia(String nick, String friend) {
        if (utenti.get(friend) == null) throw new UserNotExists();
        //aggiorno la lista di nick
        utenti.computeIfPresent(nick, (key, oldU) -> {
            oldU.aggiungiAmico(friend);

           /* else {
                throw new UserAlreadyOut();
            }*/
            return oldU;
        });

        //aggiorno la lista di friend
        utenti.computeIfPresent(friend, (key, oldU) -> {
            oldU.aggiungiAmico(nick);
           /* else {
                throw new UserAlreadyOut();
            }*/
            return oldU;
        });



    }

    /**
     * Aggiorna il punteggio e restituisce il valore aggiornato
     *
     * @param nick
     * @param punteggio
     * @return
     */
    public int aggiornaPunteggio(String nick, int punteggio) {
        return utenti.computeIfPresent(nick, (key, oldU) -> {

            //if (oldU.isLogged()) { //todo set punteggio;
            oldU.setPunteggio(punteggio);

            //}
            return oldU;
        }).getPunteggio();
    }

    /**
     * Restituisce il punteggio dell'utente
     * @param nick
     * @return
     */
    public int mostraPunteggio(String nick) {
        return utenti.get(nick).getPunteggio();
    }



    /**
     * Crea la sfida tra i due utenti
     * @param n1
     * @param n2
     */
    public void sfida(String n1, String n2){
        //todo se uno si disconnette quello che rimane continua da solo
        //TODO genera pacchetto udp e inoltra la richiesta (utilizzare future?)
       //todo synchronize
        if(isLogged(n1) && isLogged(n1)) {
            if (!hasSfidaAttiva(n1) && !hasSfidaAttiva(n1)) {
                if(richiestaSfida()) {
                    List<String> parole = getParole();
                    Sfida s = new Sfida(n1, n2, parole);
                    utenti.get(n1).setSfida(s);
                    utenti.get(n2).setSfida(s);
                }
                else
                    throw new Declined();
            }else
                throw new UserAlreadyInGame();
        }
        else
            throw new UserNotOnline();

    }

    /**
     * Effettua la richiesta della sfida tramite UDP e aspetta la risposta
     * @return true se viene accettata false altrimenti
     */
    private boolean richiestaSfida(){

    }

    /**
     * prende k parole random dal dizionario
     * @return
     */
    private List<String> getParole() {
        List<String> parole=new ArrayList<>(k);
        for(int i=0; i<k; i++){
            String parola=dict.get((int) (Math.random()*(dict.size()-1)));
            while(parole.contains(parola)){
                parola=dict.get((int) (Math.random()*(dict.size()-1)));
            }
            parole.add(parola);
        }
        return parole;
    }

    /**
     * Restituisce la lista delle richieste di amicizia
     * @param nick
     * @return
     */
    public JSONArray listaRichiesteAmicizia(String nick) {
        return utenti.get(nick).getRichiesteAmicizia();

    }

    /**
     * Restituisce la lista degli amici
     * @param nick
     * @return
     */
    public JSONArray listaAmici(String nick) {
        return utenti.get(nick).getAmici();
    }

    /**
     * Restituisce la classifica formata dagli amici dell'utente
     * @param nick
     * @return
     */
    public JSONArray mostraClassifica(String nick){
        String[] amici=utenti.get(nick).getFriendsArray();
        List<Utente> ut = new ArrayList<Utente>();
        for(int i=0; i<amici.length; i++){
            ut.add(utenti.get(amici[i]));
        }


        ut.sort(Comparator.comparingInt(Utente::getPunteggio));

        JSONArray array=new JSONArray();
        for (Utente u:ut) {
            JSONObject user=new JSONObject();
            user.put("nick", u.getNickname());
            user.put("punteggio", u.getPunteggio());
            array.add(user);
        }
        System.out.println("array:"+array.toJSONString());
        return array;

    }

    public boolean hasSfidaAttiva(String nick){
        return utenti.get(nick).getSfida()!=null;
    }

    public Utente getUtente(String nick){
        return utenti.get(nick);
    }

    public boolean isLogged(String nick){
        return utenti.get(nick).isLogged();
    }

    public boolean exists(String nick) {
        return utenti.get(nick)!=null;
    }

    public void removePendingFriend(String nick, String friend) {
        utenti.get(friend).removePending(nick);
    }



    public int getPendingSize(String nick){return utenti.get(nick).getPendingSize();}
    public Iterator<Utente> getIterator() {
        return utenti.values().iterator();
    }


}
