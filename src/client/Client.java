package client;

import Settings.Settings;
import exceptions.WrongCredException;

import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private ClientRMI rmi;
    private ClientTCP tcp;
    private ClientUDP udp;
    private Thread tcpTH;
    private Thread udpTH;
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
    private ConcurrentHashMap<String, LocalDateTime> richiesteSfida;
    private List<String> sfida;

    public Client(){
        pendingFriendsList=Collections.synchronizedList(new LinkedList<>());
        friendsList=Collections.synchronizedList(new LinkedList<>());
        classificaList=Collections.synchronizedList(new LinkedList<>());
        richiesteSfida=new ConcurrentHashMap<>();
        logged=false;
        sfida=Collections.synchronizedList(new LinkedList<>());
        rmi = new ClientRMI(8082);
        tcp=new ClientTCP(sfida, pendingFriendsList, friendsList, classificaList, richiesteSfida);
        udp=new ClientUDP(richiesteSfida);
        //todo last response?? cosi posso fare show di NOK


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
                        //todo aggiungere flag online?
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
        Thread richiesteSfidaTH=new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    synchronized (c.richiesteSfida){
                        try {

                            c.richiesteSfida.wait();
                            c.cloggedGUI.clearRichiesteSfida();
                            for (String nick:c.richiesteSfida.keySet()) {
                                c.cloggedGUI.addSfidaTile(nick);

                            }
                            c.cloggedGUI.updateUI();


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        Thread sfidaTH=new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    synchronized (c.sfida){
                        try {

                            c.sfida.wait();
                            if(c.sfida.isEmpty()){

                            }
                            else if(c.sfida.size()<3) {
                                c.cloggedGUI.initSfida(c.sfida.get(0), c.sfida.get(1));
                            }
                            else
                            {

                                c.cloggedGUI.endSfida(c.sfida.get(2));
                                c.sfida.clear();
                            }
                            c.cloggedGUI.updateUI();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        amiciziaTH.start();
        pendingTH.start();
        classificaTH.start();
        richiesteSfidaTH.start();
        sfidaTH.start();
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
        c.udp.setLoggedInfo(c.loggedNick, c.token);
        c.cloggedGUI=new ClientLoggedGUI(c.tcp, c.udp, c.loggedNick);
        Thread tcpTH=new Thread(c.tcp);
        tcpTH.start();
        Thread udpTH=new Thread(c.udp);
        udpTH.start();


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
            String response=tcp.login(nick, pw, udp.getUdpPort());
            String[] tokens=response.split(" ");
            if(tokens[0].equals("OK")){
                loggedNick=nick;

                token=tokens[2];

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
