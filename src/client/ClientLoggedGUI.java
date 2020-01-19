package client;

import client.customComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ClientLoggedGUI {
    private JPanel Friend;
    private JPanel Main;
    private JPanel Menu;
    private JScrollPane pendingRequest;
    private JScrollPane friendsScroll;
    private JTextField addFriendTB;
    private JButton addBT;
    private JPanel AddFriendP;
    private JLabel Name;
    private JButton logoutButton;
    private JList ClassificaList;
    private JScrollPane Classifica;
    private JPanel LoggedPanel;
    private JPanel pending;
    private JPanel friends;
    private JScrollPane SfideScroll;
    private JPanel Sfide;
    private ClientTCP tcp;
    private ClientUDP udp;

    private JFrame window;

    public ClientLoggedGUI(ClientTCP tcp, ClientUDP udp, String nick){
        this.tcp=tcp;
        this.udp=udp;
        window = new JFrame("ClientLoggedGUI");
        window.setContentPane(this.LoggedPanel);
        window.setSize(800,600);
        Menu.setPreferredSize(new Dimension((int)(window.getWidth()*0.3), Menu.getHeight()));
        Main.setPreferredSize(new Dimension((int)(window.getWidth()*0.3), Main.getHeight()));
        Main.setLayout(new GridLayout(1,1));
        Friend.setPreferredSize(new Dimension((int)(window.getWidth()*0.3), Friend.getHeight()));
        Classifica.setPreferredSize(new Dimension(Classifica.getWidth(), (int)(Menu.getHeight()*0.2)));
        SfideScroll.setPreferredSize(new Dimension(SfideScroll.getWidth(), (int)(Menu.getHeight()*0.2)));
        pendingRequest.setPreferredSize(new Dimension(pendingRequest.getWidth(), (int)(Friend.getHeight()*0.2)));
        friendsScroll.setPreferredSize(new Dimension(friendsScroll.getWidth(), (int)(Friend.getHeight()*0.2)));
        //pending.setPreferredSize(new Dimension(pendingRequest.getWidth(), pending.getHeight()));
        pending.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        friends.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        Sfide.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        window.setLocation(100,100);
        window.setVisible(true);
        //pending.setLayout(new GridBagLayout());
        //friends.setLayout(new GridLayout(0, 1));
        Name.setText(nick);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addBT.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String response=tcp.aggiungiAmico(addFriendTB.getText());
                String[] tokens=response.split(" ");
                if(tokens[0].equals("OK")){
                    JOptionPane.showMessageDialog(window, tokens[0]);
                }
                else if(tokens[0].equals("NOK")){
                    JOptionPane.showMessageDialog(window, tokens[1]);
                }
            }
        });



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
                if(true/*todo not già richiesta*/){
                    tcp.inviaSfida(nick);
                    String[] tokens=tcp.getResponse().split(" ");
                    if(tokens[0].equals("OK")){
                        JOptionPane.showMessageDialog(window, tokens[0]);
                    }
                    else if(tokens[0].equals("NOK")){
                        JOptionPane.showMessageDialog(window, tokens[1]);
                    }

                }
            }
        };
        friends.add(new FriendTile(nick, sListener));
        //friends.updateUI();

    }

    public void addSfidaTile(String friend) {
        ActionListener aListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //todo accetta sfida
                tcp.accettaSfida(friend);
            }
        };
        ActionListener rListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                tcp.rifiutaSfida(friend);
            }
        };
        Sfide.add(new RichiestaSfidaTile(friend, aListener, rListener));
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

    public void clearRichiesteSfida() {
        Sfide.removeAll();
    }


    public void initSfida(String friend, String parola) {
        Main.removeAll();
        SfidaTile s=new SfidaTile(friend, parola);
        ActionListener sListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String r = tcp.inviaTraduzione(s.getTraduzione());
                    try{
                        Integer.parseInt(r);

                    }catch(NumberFormatException ex){
                        s.setWord(r);
                    }

                }catch (Exception ex){
                    JOptionPane.showMessageDialog(window, ex.getMessage());
                }

            }
        };
        s.setListener(sListener);
        Main.add(s);

    }

    public void endSfida(String score) {
        try{
            int s=Integer.parseInt(score);
            JOptionPane.showMessageDialog(window, "HAI TOTALIZZATO: "+s+" PUNTI");

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        Main.removeAll();
    }
}
