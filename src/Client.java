import client.ClientRMI;
import client.ClientTCP;

import java.util.Scanner;

public class Client {
    private ClientRMI rmi;
    private ClientTCP tcp;
    private Scanner sc;
    private String token;
    private boolean logged;
    private String loggedNick;
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

        Client c=new Client();
        while(true) {
            while (!c.isLogged()) {
                c.menuOut();

                c.manageSceltaOut();
            }
            while (c.isLogged()) {
                c.menuIn();

                c.manageSceltaIn();
            }
        }
    }

    public void menuOut(){
        System.out.println("1)Registra utente");
        System.out.println("2)Login");
    }
    public void menuIn(){
        System.out.println("1)Aggiungi amico");
        System.out.println("2)Sfida");
        System.out.println("3)Logout");
    }

    public void manageSceltaOut(){
        int s=sc.nextInt();
        switch(s){
            case 1:
                registraUtente();
                break;
            case 2:
                login();
                break;
        }
    }

    public void manageSceltaIn(){
        int s=sc.nextInt();
        switch(s){
            case 1:
                aggiungiAmico();
                break;
            case 2:
                inviaSfida();
                break;
        }
    }

    public void registraUtente(){
        System.out.println("Inserisci il nome utente");
        String nick=sc.next();
        System.out.println("Inserisci la password");
        String pw=sc.next();
        String response=rmi.registraUtente(nick, pw);
        if(response.length()==32){
            loggedNick=nick;
            logged=true;
            token=response;
        }
        System.out.println(response);
    }

    public void login(){
        System.out.println("Inserisci il nome utente");
        String nick=sc.next();
        System.out.println("Inserisci la password");
        String pw=sc.next();
        String response=tcp.login(nick, pw);
        if(response.length()==32){
            loggedNick=nick;
            logged=true;
            token=response;
        }
        System.out.println(response);
    }

    public void aggiungiAmico(){

    }

    public void inviaSfida(){

    }
}
