package client;

import client.customComponents.FriendRequestTile;
import client.customComponents.FriendTile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

public class ClientLoggedGUI {
    private JPanel Friend;
    private JPanel Main;
    private JPanel Menu;
    private JScrollPane pendingRequest;
    private JScrollPane friendsScroll;
    private JTextField AddFiendTB;
    private JButton AddBT;
    private JPanel AddFriendP;
    private JLabel Name;
    private JButton button1;
    private JList ClassificaList;
    private JScrollPane Classifica;
    private JPanel LoggedPanel;
    private JPanel pending;
    private JPanel friends;
    private ClientTCP tcp;
    private Thread tcpTH;
    private JFrame window;

    public ClientLoggedGUI(ClientTCP tcp, String nick){
        this.tcp=tcp;
        this.tcp.setGUI(this);
        tcpTH=new Thread(tcp);
        tcpTH.start();
        window = new JFrame("ClientGUI");
        window.setContentPane(this.LoggedPanel);
        window.setSize(800,600);
        window.setLocation(100,100);
        window.setVisible(true);
        pending.setLayout(new GridLayout(0, 1));
        friends.setLayout(new GridLayout(0, 1));
        Name.setText(nick);




    }




    /**
     * Aggiunge alla lista delle richieste ricevute
     * @param nick
     */
    public void addPendingFriendTile(String nick){
        //pendingRequest.add(new FriendRequestTile(nick));
        //pendingRequest.getViewport().add(new FriendRequestTile(nick), null);
        //pendingRequest.revalidate();

        ActionListener aListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(tcp.accettaAmico(nick)){
                   int index=getPendingTileIndex(nick);
                    pending.remove(index);
                    pending.updateUI();
                    addFriendTile(nick);
                }

            }
        };

        ActionListener dListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(tcp.rifiutaAmico(nick)){
                    int index=getPendingTileIndex(nick);
                    pending.remove(index);
                    pending.updateUI();

                }

            }
        };
        pending.add(new FriendRequestTile(nick, aListener, dListener));



    }

    public int getPendingTileIndex(String nick){
        Component[] c=pending.getComponents();
        for(int i=0; i<c.length; i++){
            if(((FriendRequestTile)c[i]).getNick().equals(nick))
                return i;
        }
        return -1;
    }

    /**
     * Aggiunge alla lista delle amicizie
     * @param nick
     */
    public void addFriendTile(String nick){

        friends.add(new FriendTile(nick));
        friends.updateUI();

    }


    public void updateClassifica(Vector<String> list){
        ClassificaList.setListData(list);

    }

    public void boh() {
        pending.add(Box.createVerticalGlue());
    }

    public void setPendingRow(int size) {
        //pending.setLayout(new GridLayout(size,1));
    }
}
