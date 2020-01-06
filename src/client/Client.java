package client;

import javax.swing.*;

public class Client {
    private ClientRMI rmi;
    private ClientTCP tcp;
    private String token;
    private boolean logged;
    private String loggedNick;
    private Thread tFriend;
    private int pendingSize;

    public Client(){
        rmi = new ClientRMI(8082);
        //tcp deve aggiornare sulla GUI
        tcp=new ClientTCP();
        Thread tcpTh=new Thread(tcp);
        logged=false;
    }



    public String registraUtente(String nick, String pw) {

        String response=rmi.registraUtente(nick, pw);
        System.out.print(response);
        String[] tokens=response.split(" ");

        if(tokens[0].equals("NOK")){

        }
        else if(tokens[0].equals("OK")){
            loggedNick=nick;
            logged=true;
            token=response.split(" ")[1];
            pendingSize=0;
            synchronized (this) {
                this.notify();
            }
        }
        else{

        }
        return response;

    }

    public static void main(String[] args){

        Client c=new Client();
        ClientLoginGUI cloginGUI=new ClientLoginGUI(c);
        synchronized (c){
            while(!c.isLogged()){
                try {
                    c.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        cloginGUI.close();
        ClientLoggedGUI cloggedGUI=new ClientLoggedGUI();
    }

    private boolean isLogged() {
        return logged;
    }

    public String loginUtente(String nick, String pw) {

            String response=tcp.login(nick, pw);
            String[] tokens=response.split(" ");
            if(tokens[0].equals("OK")){
                loggedNick=nick;
                logged=true;
                token=tokens[1];
                pendingSize=Integer.parseInt(tokens[2]);
            }
            else {
                //todo
            }
            return response;

    }
}
