package server;

import Settings.Settings;
import exceptions.ChallengeException;
import exceptions.UserNotExists;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Challenge {
    //todo challenge factory?
    private static final int k = 10; //ms
    private static final int timer = 600; //ms
    private List<User> users;
    private int[] punteggio;
    private int[] wordN;
    private List<String> parole;
    private List<String> traduzioni;
    private AtomicBoolean active;
    //todo aggiornare punteggi quando termina la sfida
    public Challenge(User n1, User n2, List<String> parole) {
        users=new ArrayList<>(){
            @Override
            public int indexOf(Object o) {
                for (int i=0; i<users.size(); i++) {
                    if(users.get(i).getNickname().equals(o)) return i;
                }
                return -1;
            }
        };
        traduzioni=new ArrayList<>();
        active=new AtomicBoolean(true);
        users.add(n1);
        users.add(n2);
        punteggio=new int[]{0 , 0};
        wordN=new int[]{0, 0};
        this.parole=parole;
        for (String p:parole) {
            traduzioni.add(richiediTraduzione(p));
        }
    }

    public boolean isActive(){
        return active.get();
    }

    /**
     * Aggiorna anche il punteggio per il vincitore
     * In caso di pareggio nessuno riceve i punti extra
     * @return true se è terminata false se già lo era
     */
    public boolean endChallenge(){
        if(active.get()) {
            active.set(false);
            if (punteggio[0] > punteggio[1]) {
                punteggio[0] += Settings.Z;

            } else if (punteggio[1] > punteggio[0]) {
                punteggio[1] += Settings.Z;

            }
            return true;
        }
        else return false;
    }

    /**
     * Return null se sono terminate le parole
     * @param nick
     * @param word
     * @return
     */
    public String tryWord(String nick, String word){
        int i=users.indexOf(nick);
        if(i!=-1){
            synchronized (active) {
                if(active.get()) {
                    if (word.equals(traduzioni.get(wordN[i]++))) {
                        punteggio[i] += Settings.X;
                    } else {
                        punteggio[i] -= Settings.Y;
                    }

                    if (wordN[i] == Settings.k) {
                        //endChallenge();
                        return null;
                    } else {
                        return parole.get(wordN[i]);
                    }
                }
                else return null;
            }
        }
        return null;
    }

    public int getScore(String nick) throws UserNotExists {
        int i=users.indexOf(nick);
        if(i!=-1){
            return punteggio[i];
        }
        throw new UserNotExists();
    }

    public String getOpponent(String nick) throws ChallengeException {
        int i=users.indexOf(nick);
        if (i != -1) {
            switch (i){
                case 0:
                    return users.get(1).getNickname();
                case 1:
                    return users.get(0).getNickname();
            }

        }
        throw new ChallengeException();
    }


    public String getWord(int i) {
        return this.parole.get(i);
    }

    private String richiediTraduzione(String parola){

        try {

            URL url = new URL("https://api.mymemory.translated.net/get?q="+parola+"&langpair=it|en");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output=br.readLine();

            conn.disconnect();
            return parseOutput(output);
        } catch (IOException e) {

            e.printStackTrace();
            return null;
        }
    }

    private String parseOutput(String output) {
        try {
            JSONObject response = (JSONObject) new JSONParser().parse(output);
            return (String)((JSONObject)response.get("responseData")).get("translatedText");
        }catch (ParseException e) {
            return null;
        }
    }

    public synchronized List<String> getUsers(){
        List<String> r=new ArrayList<>();
        for (User u:users) {
            r.add(u.getNickname());
        }
        return r;
    }
}
