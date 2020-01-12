package client;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
    private ClientLoginGUI cloginGUI;


    public Client(){
        pendingFriendsList=Collections.synchronizedList(new LinkedList<>());
        friendsList=Collections.synchronizedList(new LinkedList<>());
        logged=false;

        rmi = new ClientRMI(8082);
        tcp=new ClientTCP(pendingFriendsList, friendsList);




    }



    public String registraUtente(String nick, String pw) {

        String response=rmi.registraUtente(nick, pw);
        System.out.print(response);
        String[] tokens=response.split(" ");

        if(tokens[0].equals("NOK")){

        }
        else if(tokens[0].equals("OK")){
            loggedNick=nick;

            token=response.split(" ")[1];

            //pendingSize=0;
            setLogged(token, nick);

        }
        else{

        }
        return response;

    }

    public static void main(String[] args){
/**      /"\
        |\./|
        |   |
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
