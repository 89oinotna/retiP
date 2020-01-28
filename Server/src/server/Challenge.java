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
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Challenge extends IChallenge implements Runnable{
    private List<User> users;
    private int[] punteggio;
    private int[] wordN;
    private List<String> parole;
    private List<String> traduzioni;
    private AtomicBoolean active;
    private static List<String> dict;
    private SelectionKey k1;
    private SelectionKey k2;
    private ConcurrentHashMap<SelectionKey,SelectionKey> usingK;
    public Challenge(User n1, User n2) {
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
        this.parole=getParole();

    }

    public boolean isActive(){
        return active.get();
    }

    public synchronized boolean endChallenge(){
        if(active.get()) {
            active.set(false);
            if (punteggio[0] > punteggio[1]) {
                punteggio[0] += Settings.Z;

            } else if (punteggio[1] > punteggio[0]) {
                punteggio[1] += Settings.Z;

            }

            users.get(0).addScore(punteggio[0]);
            users.get(1).addScore(punteggio[1]);

            return true;
        }
        else return false;
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

    /**
     * Controlla se word corrisponde alla traduzione e aggiorna i punteggi
     * @param nick che invia la traduzione
     * @param word parola tradotta
     * @return la prossima parola da tradurre, null se non ci sono piu parole o la sfida non è attiva
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

    public String getWord(int i) {
        return this.parole.get(i);
    }

    /**
     * Effettua la traduzione della parola
     * @param parola parola da tradurre
     * @return parola tradotta, null se non riesce a tradurla
     */
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

    /**
     * Estrapola la parola tradotta da output
     * @param output json risultato della richesta API
     * @return traduzione
     */
    private String parseOutput(String output) {
        try {
            JSONObject response = (JSONObject) new JSONParser().parse(output);
            return (String)((JSONObject)response.get("responseData")).get("translatedText");
        }catch (ParseException e) {
            return null;
        }
    }

    /**
     * Prende k parole diverse random dal dizionario
     * @return lista delle parole
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

    /**
     * Creazione del dizionario
     * @param dict lista delle parole
     */
    public static void setDict(List<String> dict){
        Challenge.dict =dict;
    }

    @Override
    public void run() {
        for (String p:parole) {
            //todo gestione return null
            try {
                traduzioni.add(richiediTraduzione(p));
            }catch(NullPointerException ignored){}
        }
        MyAttachment ak1=((MyAttachment)k1.attachment());
        MyAttachment ak2=((MyAttachment)k2.attachment());
        try {
            send(k1, Settings.RESPONSE.SFIDA + " " + ak1.getToken() +
                    " " + ak2.getNick() + " " + Settings.SFIDA.INIZIATA + " " + getWord(0) + "\n");
        }catch (IOException ignored){
        }
        try {
            send(k2, Settings.RESPONSE.SFIDA + " " + ak2.getToken() +
                    " " + ak1.getNick() + " " + Settings.SFIDA.INIZIATA+" "+getWord(0)+"\n");
        } catch (IOException ignored) {
        }
    }

    public void setKeys(SelectionKey k1, SelectionKey k2, ConcurrentHashMap<SelectionKey,SelectionKey> usingK){
        this.usingK=usingK;
        this.k1=k1;
        this.k2=k2;
    }

    /**
     * Scrive sul channell associato a k
     *
     * @param k
     * @param response
     * @throws IOException
     */
    public void send(SelectionKey k, String response) throws IOException {
        //todo send scrittura multiplo di BUFFLEN
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buf = ByteBuffer.wrap( response.getBytes() );
        synchronized (usingK) {
            while(usingK.containsKey(k)){
                try {
                    usingK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            usingK.put(k, k);
        }
        while(buf.hasRemaining()){
            client.write(buf);
        }
        synchronized (usingK) {
            usingK.remove(k, k);
            usingK.notify();
        }


    }
}
