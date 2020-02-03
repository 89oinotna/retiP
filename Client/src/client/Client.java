package client;


import client.GUI.ClientLoggedGUI;
import client.GUI.ClientLoginGUI;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    private final ClientRMI rmi;
    private final ClientTCP tcp;
    private final ClientUDP udp;
    private Thread tcpTH;
    private Thread udpTH;
    private boolean logged;
    private ClientLoggedGUI cloggedGUI;
    private ConcurrentHashMap<String, String> richiesteSfida;
    private List<String> sfida;
    private List<String> pendingFriendsList; //lista delle richieste di amicizia ricevute
    private List<String> friendsList; //lista degli amici
    private final List<String> classificaList; //classifica
    private ClientLoginGUI cloginGUI;


    public Client(){
        pendingFriendsList=Collections.synchronizedList(new LinkedList<>());
        friendsList=Collections.synchronizedList(new LinkedList<>());
        classificaList=Collections.synchronizedList(new LinkedList<>());
        richiesteSfida=new ConcurrentHashMap<>();
        logged=false;
        sfida=Collections.synchronizedList(new LinkedList<>());
        rmi = new ClientRMI(8082);
        tcp = new ClientTCP(sfida, pendingFriendsList, friendsList, classificaList, richiesteSfida);
        udp = new ClientUDP(richiesteSfida);


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

        while(true) {
            Client c = new Client();
            c.cloginGUI = new ClientLoginGUI( c.rmi, c.tcp, c.udp);
            synchronized (c.tcp) {
                while (!c.tcp.isLogged()) {
                    try {
                        c.tcp.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            c.cloginGUI.close();
            c.cloggedGUI = new ClientLoggedGUI(c.tcp, c.tcp.getLoggedNick(), c.sfida, c.pendingFriendsList, c.friendsList, c.classificaList, c.richiesteSfida);
            c.tcpTH = new Thread(c.tcp);
            c.udpTH = new Thread(c.udp);
            c.tcpTH.start();
            c.udpTH.start();
            synchronized (c.tcp) {
                while (c.tcp.isLogged()) {
                    try {
                        c.tcp.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            c.tcpTH.interrupt();
            c.udpTH.interrupt();
            c.cloggedGUI.close();
        }
    }
}
