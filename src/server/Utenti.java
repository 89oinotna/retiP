package server;


import exceptions.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Utenti implements Serializable {
    ConcurrentHashMap<String, Utente> utenti;

    public Utenti() {
        utenti = new ConcurrentHashMap<>();

    }

    public Utenti(JSONArray utentiJSON) {
        utenti = new ConcurrentHashMap<>();
        for (Object utenteJSON : utentiJSON) {
            //todo threadpool
            Utente curr = new Utente((JSONObject) utenteJSON);
            utenti.put(curr.getNickname(), curr);
        }
    }

    public String registraUtente(String nick, String pw) throws UserAlreadyExists {
        if ((utenti.putIfAbsent(nick, new Utente(nick, pw))) != null) throw new UserAlreadyExists();
        else {
            return generateToken(nick, pw);
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
            return u.getToken().equals(token);
        }
        else throw new UserNotExists();
    }

    public String loginUtente(String nick, String pw, SelectionKey k) {
        //Utente u = utenti.get(nick);
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
    public void aggiungiAmico(String nick, String friend) {
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

    public int getPunteggio(String nick) {
        return utenti.get(nick).getPunteggio();
    }

    public Iterator<Utente> getIterator() {
        return utenti.values().iterator();
    }

    public void creaSfida(String n, String m){
        Sfida s=new Sfida();
        utenti.get(n).setSfida(s);
        utenti.get(m).setSfida(s);
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
}
