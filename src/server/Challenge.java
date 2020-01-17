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
    private List<String> nick;
    private int[] punteggio;
    private int[] wordN;
    private List<String> parole;
    private List<String> traduzioni;
    private AtomicBoolean active;
    public Challenge(String n1, String n2, List<String> parole) {
        nick=new ArrayList<>();
        traduzioni=new ArrayList<>();
        active=new AtomicBoolean(true);
        nick.add(n1);
        nick.add(n2);
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

    public void endChallenge(){
        active.set(false);
    }

    public String tryWord(String nick, String word){
        int i=nick.indexOf(nick);
        if(i!=-1){
            synchronized (active) {
                if(active.get()) {
                    if (word.equals(traduzioni.get(wordN[i]++))) {
                        punteggio[i] += Settings.X;
                    } else {
                        punteggio[i] -= Settings.Y;
                    }
                    if (wordN[i] == Settings.k) {
                        endChallenge();
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
        int i=nick.indexOf(nick);
        if(i!=-1){
            return punteggio[i];
        }
        throw new UserNotExists();
    }

    public String getOpponent(String nick) throws ChallengeException {
        int i=this.nick.indexOf(nick);
        if (i != -1) {
            switch (i){
                case 0:
                    return this.nick.get(1);
                case 1:
                    return this.nick.get(0);
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
}
