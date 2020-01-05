import client.ClientRMI;
import client.ClientTCP;
import client.NonBlockingIO;

import java.util.Scanner;

public class Client {
    private ClientRMI rmi;
    private ClientTCP tcp;
    private Scanner sc;
    private String token;
    private boolean logged;
    private String loggedNick;
    private Thread tFriend;
    private NonBlockingIO nbIO;
    private int pendingSize;
    public Client(){
        rmi = new ClientRMI(8082);
        tcp=new ClientTCP();
        logged=false;
        sc=new Scanner(System.in);
    }
    public boolean isLogged(){
        return logged;
    }

    public static void main(String[] args) {
        String scelta;
        Client c=new Client();
        c.nbIO=new NonBlockingIO(c.sc);
        Thread nbIOTh=new Thread(c.nbIO);
        nbIOTh.start();
        while(true) {
            //non loggato
            while (!c.isLogged()) {
                c.menuOut();
                c.manageSceltaOut();
                if(c.isLogged()) c.menuIn();

            }
            String r="";

            //ciclo che controlla se ho ricevuto una richiesta di amicizia o
            //è stata selezionata una voce dal menu
            while (c.isLogged() && (r=c.tcp.read())==null /*&& !udp request sfida*/) {
                //todo migliorare in qualche modo?
                if((scelta=c.nbIO.getLine())!=null){
                    c.manageSceltaIn(scelta);
                    c.menuIn();
                }


            }
            //todo r potrebbe contenere piu di una richiesta????
            String[] tokens=r.split(" "); //OK friend list potrebbe restare da leggere
            //todo scorri tutti i token per vedere se c'è qualcosa
            if(tokens[0].equals("AMICIZIA")){
                //AMICIZIA nickamico token richiesta/accettata/rifiutata
                //todo validate token
                String response = c.manageAmicizia(tokens[1], tokens[3]); //AMICIZIA nick(client) token nick(amico) accetta/rifiuta
                if(response!=null)c.tcp.send(response);

            }
        }
    }

    public void menuOut(){
        System.out.println("1)Registra utente");
        System.out.println("2)Login");
    }
    public void menuIn(){
        System.out.println("1)Aggiungi amico");
        System.out.println("2)Sfida amico");
        System.out.println("3)Richieste di amicizia "+pendingSize);
        System.out.println("4)Logout");
    }

    public void manageSceltaOut(){
        int s=Integer.parseInt(nbIO.getLineBlocking());//sc.nextInt();
        switch(s){
            case 1:
                registraUtente();
                break;
            case 2:
                login();
                break;
        }
    }

    public void manageSceltaIn(String scelta){
        int s=Integer.parseInt(scelta);//sc.nextInt();

        switch(s){
            case 1:
                aggiungiAmico();
                break;
            case 2:
                inviaSfida();
                break;
            case 3:
                pendingFriends();
        }
    }

    public void registraUtente(){
        System.out.println("Inserisci il nome utente");
        String nick=nbIO.getLineBlocking();
        System.out.println("Inserisci la password");
        String pw=nbIO.getLineBlocking();


        String response=rmi.registraUtente(nick, pw);
        if(!response.contains("NOK")){
            loggedNick=nick;
            logged=true;
            token=response.split(" ")[1];
            pendingSize=0;
        }
        System.out.println(response);
    }

    public void login(){
        System.out.println("Inserisci il nome utente");
        String nick=nbIO.getLineBlocking();
        System.out.println("Inserisci la password");
        String pw=nbIO.getLineBlocking();
        String response=tcp.login(nick, pw);
        String[] tokens=response.split(" ");
        if(tokens[0].equals("OK")){
            loggedNick=nick;
            logged=true;
            token=tokens[1];
            pendingSize=Integer.parseInt(tokens[2]);
        }
        System.out.println(response);
    }

    /**
     * Invia la richiesta di amicizia
     */
    public void aggiungiAmico(){
        System.out.println("Inserisci il nome dell'amico da aggiungere");
        String friend=nbIO.getLineBlocking();
        String request="AMICIZIA "+loggedNick+" "+token+" "+friend+" RICHIESTA";
        tcp.send(request);
    }

    /**
     * gestisce l'invio della sfida ad un amico
     */
    public void inviaSfida(){
        System.out.println("Inserisci il nome dell'amico da sfidare");
        String friend=nbIO.getLineBlocking();
        String request="SFIDA "+loggedNick+" "+token+" "+friend;
        tcp.send(request);
    }

    /**
     * gestisce la richiesta di amicizia (stampa solo info sulla richiesta)
     * @param friend
     * @param type
     * @return
     */
    public String manageAmicizia(String friend, String type){
        //todo per accettare vado nella pagina delle richieste??
        switch(type){
            case "RICHIESTA":

                //todo show richiesta arrivata
                System.out.println("Richiesta di amicizia da "+friend);
                pendingSize++;
                //manageRichiestaAmicizia(friend);
                break;
            case "ACCETTATA":
                //boh
                break;
            case "RIFIUTATA":
                //boh
                break;
        }
        return null;
    }

    public String[] getPendingFriends(){
        String request = "GET "+loggedNick+" "+token+" PENDING FRIENDS";
        tcp.send(request);
        String response = tcp.read();
        String[] tokens=null;
        if(response!=null) {
            tokens = response.split(" ");
            if (tokens[0].equals("OK")) {

                for (int i = 1; i < tokens.length; i++) {
                    System.out.println(i + ": " + tokens[i]);
                }
            }
        }
        return tokens;
    }

    public void pendingFriends(){
        //todo fetch pending and show
        //todo se arriva richiesta di amicizia o sfida??
        String[] last=null;
        int scelta=-1;
        while(scelta<0 /*&& !udp request sfida*/) {

            String[] friends=getPendingFriends();
            System.out.println("0: INDIETRO");
            if(friends!=null && !last.equals(friends)) {
                    for (int i = 1; i < friends.length; i++) {
                        System.out.println(i + ": " + friends[i-1]);
                    }

            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String line;
            if((line=nbIO.getLine())!=null){
                scelta=Integer.parseInt(line);

                if(friends!=null && scelta>0 && scelta<=friends.length) {


                    managePendingFriend(friends[scelta - 1]);
                    if(scelta>0) {
                        System.out.println("0) INDIETRO");
                        System.out.println("1) ACCETTA");
                        System.out.println("2) RIFIUTA");
                    }
                }

            }

        }



    }
}
