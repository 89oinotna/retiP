package server;

import exceptions.FriendshipException;
import exceptions.UserAlreadyLogged;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class User implements IUser {

    private String nickname;
    private String password;
    private int score;
    private boolean logged;
    private ConcurrentHashMap<String, String> friends;
    private ConcurrentHashMap<String, String> pending;
    private Challenge challenge;
    private SelectionKey k;
    private String token;

    public User(JSONObject user) {
        friends = new ConcurrentHashMap<>();
        pending=new ConcurrentHashMap<>();
        logged = false;
        token="";
        k=null;
        nickname = user.get("nickname").toString();
        password = user.get("password").toString();
        score = Integer.parseInt(user.get("score").toString());
        JSONArray friendsJSON = (JSONArray) user.get("friends");
        for (int i = 0; i < friendsJSON.size(); i++) {
            friends.put(friendsJSON.get(i).toString(), friendsJSON.get(i).toString());
        }
        JSONArray pendingJSON = (JSONArray) user.get("pending");
        for (int i = 0; i < pendingJSON.size(); i++) {
            pending.put(pendingJSON.get(i).toString(), pendingJSON.get(i).toString());
        }

    }

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
        this.token="";
        score = 0;
        //logged = true; //si logga alla registrazione
        friends = new ConcurrentHashMap<>();
        pending=new ConcurrentHashMap<>();
        k=null;

    }

   /* public SelectionKey getK() {
        return k;
    }*/

    /*public void setK(SelectionKey k) {
        this.k = k;
    }*/

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

    public boolean isLogged() { return logged;  }

    public void login(String token) throws UserAlreadyLogged{
        if(!logged)logged = true;
        else throw new UserAlreadyLogged();
        this.token=token;
    }

    public void logout() {
        logged = false;
        token="";
    }

    /*                      FRIENDS METHOD                      */

    public void addFriend(String friend) {
        friends.putIfAbsent(friend, friend);
        removePending(friend);
    }

    public boolean removeFriend(String friend) { return (friends.remove(friend)) != null; }

    public boolean addPending(String friend) throws FriendshipException {
        if(friends.contains(friend)) throw new FriendshipException("FriendshipExists");
        return (pending.putIfAbsent(friend, friend))==null;
    }

    public void removePending(String friend){ pending.remove(friend); }

    public List<String> getFriends() {  return new ArrayList<String>(friends.keySet());  }

    public List<String> getPendings(){ return new ArrayList<String>(friends.keySet()); }


    /*                      JSON METHODS                        */

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
        for (String nome : pending.keySet()) {
            pendingJSON.add(nome);
        }
        json.put("friends", friendsJSON);
        json.put("pending", pendingJSON);

        return json;
    }




}
