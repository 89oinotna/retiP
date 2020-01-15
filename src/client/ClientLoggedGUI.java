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

    private JFrame window;

    public ClientLoggedGUI(ClientTCP tcp, String nick){
        this.tcp=tcp;

        window = new JFrame("ClientGUI");
        window.setContentPane(this.LoggedPanel);
        window.setSize(800,600);
        window.setLocation(100,100);
        window.setVisible(true);
        pending.setLayout(new GridLayout(0, 1));
        friends.setLayout(new GridLayout(0, 1));
        Name.setText(nick);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);





    }




    /**
     * Aggiunge alla lista delle richieste ricevute
     * @param nick
     */
    public void addPendingFriendTile(String nick){
        //pendingRequest.add(new FriendRequestTile(nick));
        //pendingRequest.getViewport().add(new FriendRequestTile(nick), null);
        //pendingRequest.revalidate();

        //listener per bottone che accetta
        ActionListener aListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                tcp.accettaAmico(nick);
                /*if(tcp.accettaAmico(nick)){
                   int index=getPendingTileIndex(nick);
                    //pending.remove(index);

                    addFriendTile(nick);

                }*/

            }
        };

        //listener per bottone che rifiuta
        ActionListener dListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                tcp.rifiutaAmico(nick);

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

        //listener per bottone sfida
        ActionListener sListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(true/*todo not in sfida*/){
                    if(tcp.inviaSfida(nick)){
                        //todo start sfida
                    }
                }
            }
        };
        friends.add(new FriendTile(nick, sListener));
        //friends.updateUI();

    }

    private void inizializzaSfida(){

    }


    public void updateClassifica(List<String> list){
    System.out.println("List Classifica: "+list.size());
        ClassificaList.setListData(list.toArray(new String[list.size()]));
        //ClassificaList.updateUI();
    }

    public void updateUI(){
        LoggedPanel.updateUI();
    }

    public void clearPending(){
        pending.removeAll();
    }

    public void clearFriend(){
        friends.removeAll();
    }

    public void clearClassifica(){
        ClassificaList.removeAll();
    }


}
