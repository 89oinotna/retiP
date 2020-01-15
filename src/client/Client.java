package client;

import exceptions.WrongCredException;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class Client {
    private ClientRMI rmi;
    private ClientTCP tcp;
    private Thread tcpTH;
    private String token;
    private boolean logged;
    private String loggedNick;
    private ClientLoggedGUI cloggedGUI;
    //private Thread tFriend;
    //private int pendingSize;
    //todo togliere dal server il pendingsize
    private List<String> pendingFriendsList; //lista delle richieste di amicizia ricevute
    private List<String> friendsList; //lista degli amici
    private final List<String> classificaList; //classifica
    private ClientLoginGUI cloginGUI;


    public Client(){
        pendingFriendsList=Collections.synchronizedList(new LinkedList<>());
        friendsList=Collections.synchronizedList(new LinkedList<>());
        classificaList=Collections.synchronizedList(new LinkedList<>());
        logged=false;

        rmi = new ClientRMI(8082);
        tcp=new ClientTCP(pendingFriendsList, friendsList, classificaList);




    }





    public static void main(String[] args){
/**      /"\
        |\./|
        |   |
        |>*<|
        |   |
     /'\|   |/'\
 /'\|   |   |   |
|   |   |   |   |\
|   |   |   |   |  \
| *   *   *   * |>  >
|                  /
 |               /
  |            /
   \          |
    |         |
 */
        Client c=new Client();
        c.cloginGUI=new ClientLoginGUI(c);
        //TODO LISTENER PER OGGETTO RICHIESTE aMICIZIA
        Thread amiciziaTH=new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    synchronized (c.friendsList){
                        try {
                            c.friendsList.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        c.cloggedGUI.clearFriend();
                        for (String s:c.friendsList) {
                            c.cloggedGUI.addFriendTile(s);

                        }
                        c.cloggedGUI.updateUI();
                    }
                }
            }
        });
        //TODO LISTENER OGGETTO AMICIZIA ACCETTATA
        Thread pendingTH=new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    synchronized (c.pendingFriendsList){
                        try {
                            c.pendingFriendsList.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        c.cloggedGUI.clearPending();
                        for (String s:c.pendingFriendsList) {
                            c.cloggedGUI.addPendingFriendTile(s);
                        }

                        c.cloggedGUI.updateUI();

                    }
                }
            }
        });
        Thread classificaTH=new Thread(new Runnable() {
            @Override
            public void run() {
                int size=0;
                while(!Thread.currentThread().isInterrupted()){
                    synchronized (c.classificaList){
                        try {
                            c.classificaList.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        c.cloggedGUI.clearClassifica();
                        c.cloggedGUI.updateClassifica(c.classificaList);
                        c.cloggedGUI.updateUI();


                    }
                }
            }
        });
        amiciziaTH.start();
        pendingTH.start();
        classificaTH.start();
        synchronized (c){
            while(!c.isLogged()){
                try {
                    c.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        c.cloginGUI.close();

        c.cloggedGUI=new ClientLoggedGUI(c.tcp, c.loggedNick);
        Thread tcpTH=new Thread(c.tcp);
        tcpTH.start();


        synchronized (c){
            while(!c.isLogged()){
                try {
                    c.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public String registraUtente(String nick, String pw) {

        String response=rmi.registraUtente(nick, pw);
        System.out.print(response);
        String[] tokens=response.split(" ");

        if(tokens[0].equals("NOK")){

        }
        else if(tokens[0].equals("OK")){
            return loginUtente(nick, pw);
        }
        else{
            throw new WrongCredException();
        }
        return response;

    }

    private boolean isLogged() {
        return logged;
    }

    public String loginUtente(String nick, String pw) {
            System.out.println(pw);
            String response=tcp.login(nick, pw);
            String[] tokens=response.split(" ");
            if(tokens[0].equals("OK")){
                loggedNick=nick;

                token=tokens[1];

                setLogged(token, nick);
                //pendingSize=Integer.parseInt(tokens[2]);
            }
            else {
                //todo

            }
            return response;

    }



    public synchronized void setLogged(String token, String nick){
        tcp.setToken(token);
        tcp.setLoggedNick(nick);
        logged=true;

        synchronized (this) {
            this.notify();
        }

    }


}
