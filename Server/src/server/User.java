package server;

import exceptions.FriendshipException;
import exceptions.UserAlreadyLogged;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rappresenta un utente
 */
public class User implements IUser {

    private String nickname;
    private String password;
    private int score;
    private boolean logged;
    private ConcurrentHashMap<String, String> friends;
    private ConcurrentHashMap<String, String> pendingFriend;
    private ConcurrentHashMap<String, String> challengeRequest;
    private Challenge challenge;

    private String token;

    public User(JSONObject user) {
        friends = new ConcurrentHashMap<>();
        pendingFriend =new ConcurrentHashMap<>();
        challengeRequest=new ConcurrentHashMap<>();
        logged = false;
        token="";
        nickname = user.get("nickname").toString();
        password = user.get("password").toString();
        score = Integer.parseInt(user.get("score").toString());
        JSONArray friendsJSON = (JSONArray) user.get("friends");
        for (int i = 0; i < friendsJSON.size(); i++) {
            friends.put(friendsJSON.get(i).toString(), friendsJSON.get(i).toString());
        }
        JSONArray pendingJSON = (JSONArray) user.get("pending");
        for (int i = 0; i < pendingJSON.size(); i++) {
            pendingFriend.put(pendingJSON.get(i).toString(), pendingJSON.get(i).toString());
        }

    }

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
        this.token="";
        score = 0;
        //logged = true; //si logga alla registrazione
        friends = new ConcurrentHashMap<>();
        pendingFriend =new ConcurrentHashMap<>();
        challengeRequest=new ConcurrentHashMap<>();


    }

    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public String getToken() {
        return token;
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

    public int getScore() { return score;  }

    public void setScore(int score) {
        this.score = score;
    }

    public synchronized int addScore(int i) {
        return score+=i;
    }

    public boolean isLogged() { return logged;  }

    public synchronized void login(String token) throws UserAlreadyLogged{
        if(!logged)logged = true;
        else throw new UserAlreadyLogged();
        this.token=token;
    }

    public synchronized void logout() {
        logged = false;
        token="";
    }

    /*                      FRIENDS                      */

    public boolean hasFriend(String friend) {
        return friends.get(friend)!=null;
    }

    public synchronized void addFriend(String friend) {
        friends.putIfAbsent(friend, friend);
        removePending(friend);
    }

    public synchronized boolean removeFriend(String friend) { return (friends.remove(friend)) != null; }

    public synchronized boolean addPending(String friend) throws FriendshipException {

        if(pendingFriend.putIfAbsent(friend, friend)==null)
            return true;
        throw new FriendshipException("FriendshipExists");
    }

    public synchronized void removePending(String friend){ pendingFriend.remove(friend); }

    public List<String> getFriends() {
        return new ArrayList<String>(friends.keySet());
    }

    public List<String> getPendings(){ return new ArrayList<String>(pendingFriend.keySet()); }

    /*                      CHALLENGE                   */

    /**
     * Controlla l'esistenza di una richiesta di sfida da friend
     * @param friend from
     * @return true se esiste, false altrimenti
     */
    public boolean hasChallengeRequest(String friend) {
        return challengeRequest.get(friend)!=null;
    }

    /**
     * Aggiunge una richiesta di sfida alla lista delle richieste dell'utente
     * @param friend from
     * @return true se non era già presente, false altrimenti
     */
    public synchronized boolean addChallengeRequest(String friend){
        return challengeRequest.putIfAbsent(friend, friend)==null;
    }

    /**
     * Rimuove una richiesta di sfida alla lista delle richieste dell'utente
     * @param friend from
     * @return true se era presente ed è stato rimosso, false altrimenti
     */
    public synchronized boolean removeChallengeRequest(String friend) {
        return challengeRequest.remove(friend)!=null;
    }

    /**
     * Controlla che l'utente abbia una sfida attiva
     * @return true se ce l'ha, false altrimenti
     */
    public boolean isInChallenge(){
        if(challenge==null)return false;
        synchronized (challenge) {
            return challenge.isActive();
        }
    }

    /*                      JSON                        */

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray friendsJSON = new JSONArray();
        JSONArray pendingJSON = new JSONArray();
        json.put("nickname", nickname);
        json.put("password", password);
        json.put("score", score);
        //json.put("logged", logged);
        for (String nome : friends.keySet()) {
            friendsJSON.add(nome);
        }
        for (String nome : pendingFriend.keySet()) {
            pendingJSON.add(nome);
        }
        json.put("friends", friendsJSON);
        json.put("pending", pendingJSON);

        return json;
    }
}
