package server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Utente {
    private String nickname;
    private String password;
    private int punteggio;
    private boolean logged;
    private ConcurrentHashMap<String, String> amici;
    private ConcurrentHashMap<String, String> pendingRequest;
    private Sfida sfida;
    private SelectionKey k;

    public SelectionKey getK() {
        return k;
    }

    public void setK(SelectionKey k) {
        this.k = k;
    }



    public Sfida getSfida() {
        return sfida;
    }

    public void setSfida(Sfida sfida) {
        this.sfida = sfida;
    }



    public String getToken() {
        return token;
    }

    private String token;

    public Utente(String _nickname, String _password, String _token) {
        nickname = _nickname;
        password = _password;
        punteggio = 0;
        logged = true; //si logga alla registrazione
        amici = new ConcurrentHashMap<>();
        pendingRequest=new ConcurrentHashMap<>();
        token=_token;
    }

    public Utente(JSONObject utente) {
        amici = new ConcurrentHashMap<>();
        pendingRequest=new ConcurrentHashMap<>();
        nickname = utente.get("nickname").toString();
        password = utente.get("password").toString();
        punteggio = Integer.parseInt(utente.get("punteggio").toString());
        logged = false;
        JSONArray amiciJSON = (JSONArray) utente.get("amici");
        for (int i = 0; i < amiciJSON.size(); i++) {
            amici.put(amiciJSON.get(i).toString(), amiciJSON.get(i).toString());
        }
        JSONArray pendingJSON = (JSONArray) utente.get("pending");
        for (int i = 0; i < pendingJSON.size(); i++) {
            pendingRequest.put(pendingJSON.get(i).toString(), pendingJSON.get(i).toString());
        }
        token="";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getPunteggio() {
        return punteggio;
    }

    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
    }

    public boolean isLogged() {

        return logged;
    }

    public void logIn(String _token) {


            logged = true;
            token=_token;


    }

    public void logOut() {

        if (logged) {
            logged = false;
        }

    }

    public void aggiungiAmico(String nick) {
        amici.putIfAbsent(nick, nick);
        removePending(nick);
    }

    public void removePending(String nick){
        pendingRequest.remove(nick);
    }

    public boolean rimuoviAmico(String nick) {
        return (amici.remove(nick)) != null;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray amiciJSON = new JSONArray();
        JSONArray pendingJSON = new JSONArray();
        json.put("nickname", nickname);
        json.put("password", password);
        json.put("punteggio", punteggio);
        //json.put("logged", logged);
        for (String nome : amici.keySet()) { //todo lo richiama ogni volta?
            amiciJSON.add(nome);
        }
        for (String nome : pendingRequest.keySet()) { //todo lo richiama ogni volta?
            pendingJSON.add(nome);
        }
        json.put("amici", amiciJSON);
        json.put("pending", pendingJSON);

        return json;
    }

    /**
     * aggiunge alla pending list degli amici
     * @param nick
     * @return true se non era presente false altrimenti
     */
    public boolean addPending(String nick) {
        return (pendingRequest.putIfAbsent(nick, nick))==null;
    }


    public int getPendingSize() {
        return pendingRequest.size();
    }

    /*public Iterator<String> getPending() {
        return pendingRequest.keySet().iterator();
    }*/

    public JSONArray getAmici() {
        JSONArray array=new JSONArray();

        array.addAll(amici.keySet());
        return array;
    }

    public String[] getFriendsArray() {

        return (String[]) amici.keySet().toArray();
    }

    public JSONArray getRichiesteAmicizia() {

        JSONArray array=new JSONArray();

        array.addAll(pendingRequest.keySet());
        return array;
    }
}
