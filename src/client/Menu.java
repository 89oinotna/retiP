package client;

import java.util.Scanner;

public class Menu implements Runnable{
    private ClientInfo info;
    private Scanner sc;
    public Menu(ClientInfo _info){
        info=_info;
    }
    public void menuOut(){
        System.out.println("1)Registra utente");
        System.out.println("2)Login");
    }
    public void menuIn(){
        System.out.println("1)Aggiungi amico");
        System.out.println("2)Sfida amico");
        System.out.println("3)Richieste di amicizia "+info.getPendingSize());
        System.out.println("4)Logout");
    }

    public void manageSceltaOut(){
        int s=Integer.parseInt(sc.next());//sc.nextInt();
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
        String nick=sc.next();
        System.out.println("Inserisci la password");
        String pw=sc.next();


        String response=rmi.registraUtente(nick, pw);
        if(!response.contains("NOK")){
            info.setLoggedNick(nick);
            info.setLogged(true);
            info.setToken(response.split(" ")[1]);
            info.setPendingSize=0;
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

    @Override
    public void run() {

    }
}
