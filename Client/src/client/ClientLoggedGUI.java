package client;

import Settings.Settings;
import client.customComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
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
    private JButton logoutBT;
    private JList<String> ClassificaList;
    private JScrollPane Classifica;
    private JPanel LoggedPanel;
    private JPanel pending;
    private JPanel friends;
    private JScrollPane SfideScroll;
    private JPanel Sfide;
    private JLabel pendingLB;
    private JLabel friendsLB;
    private JLabel challengeLB;
    private JLabel leaderboardLB;
    private ClientTCP tcp;
    private JFrame window;

    public ClientLoggedGUI(ClientTCP tcp, String nick){
        this.tcp=tcp;
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
        pending.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        friends.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        Sfide.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        window.setLocation(100,100);
        window.setVisible(true);
        Name.setText(nick);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addBT.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String response = tcp.aggiungiAmico(addFriendTB.getText());
                    String[] tokens = response.split(" ");
                    if (Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
                        JOptionPane.showMessageDialog(window, tokens[0]);
                    } else if (Settings.RESPONSE.NOK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
                        JOptionPane.showMessageDialog(window, tokens[1]);
                    }
                }catch(IOException ex){
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }
            }
        });

        logoutBT.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tcp.logout();
                logout();
            }
        });



    }

    /**
     * Chiude la finestra
     */
    public void close() {
        window.dispose();
    }

    /**
     * Effettua l'update della UI
     */
    public void updateUI(){
        LoggedPanel.updateUI();
    }

    /**
     * Effettua il logout resettando tcp
     */
    private void logout(){
        synchronized (tcp) {
            tcp.setLoggedNick(null);
            tcp.setToken(null);
            tcp.notify();
        }
    }

    /*                      RICHIESTE AMICIZIA                      */

    /**
     * Aggiunge alla lista delle richieste ricevute
     * @param friend da chi viene la richiesta
     */
    public void addPendingFriendTile(String friend){
        //listener bottone che accetta
        ActionListener aListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    tcp.accettaAmico(friend);
                }catch(IOException ex){
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }
            }
        };

        //listener per bottone che rifiuta
        ActionListener dListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    tcp.rifiutaAmico(friend);
                }catch(IOException ex){
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }

            }
        };

        pending.add(new FriendRequestTile(friend, aListener, dListener));



    }

    /**
     * Pulisce la lista delle richieste
     */
    public void clearPending(){
        pending.removeAll();
    }

    /*                      AMICI                                   */

    /**
     * Aggiunge amico alla lista delle amicizie
     * @param friend amico da inserire
     */
    public void addFriendTile(String friend){

        //listener per bottone sfida
        ActionListener sListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try{
                    String[] tokens=tcp.inviaSfida(friend).split(" ");
                    if(Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))){
                        JOptionPane.showMessageDialog(window, tokens[0]);
                    }
                    else if(Settings.RESPONSE.NOK.equals(Settings.RESPONSE.valueOf(tokens[0]))){
                        JOptionPane.showMessageDialog(window, tokens[1]);
                    }

                }catch(IOException ex){
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }
            }
        };
        friends.add(new FriendTile(friend, sListener));

    }

    /**
     * Pulisce la lista amici
     */
    public void clearFriend(){
        friends.removeAll();
    }

    /*                      SFIDA                                   */

    /**
     * Aggiunge una richiesta diu sfida
     * @param friend amico che richiede la sfida
     */
    public void addSfidaTile(String friend) {
        ActionListener aListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {

                    String[] tokens=tcp.accettaSfida(friend).split(" ");
                    if(Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))){
                        preparaSfida(friend);
                        updateUI();
                    }
                    else if(Settings.RESPONSE.NOK.equals(Settings.RESPONSE.valueOf(tokens[0]))){
                        JOptionPane.showMessageDialog(window, tokens[1]);
                    }
                }catch(IOException ex){
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }
            }
        };
        ActionListener rListener= new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    tcp.rifiutaSfida(friend);
                }catch(IOException ex){
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }
            }
        };
        Sfide.add(new RichiestaSfidaTile(friend, aListener, rListener));
    }

    /**
     * Pulisce le richieste sfida
     */
    public void clearRichiesteSfida() {
        Sfide.removeAll();
    }

    /**
     * Inizia la sfida
     * @param friend sfidante
     * @param parola parola da tradurre
     */
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

                }catch (IllegalArgumentException ex){
                    JOptionPane.showMessageDialog(window, ex.getMessage());
                }catch(IOException ex){
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }

            }
        };
        s.setListener(sListener);
        Main.add(s);
    }

    /**
     * Termina la sfida
     * @param score punteggio
     */
    public void endSfida(int score) {
        JOptionPane.showMessageDialog(window, "HAI TOTALIZZATO: "+score+" PUNTI");
        Main.removeAll();
    }

    /**
     * Prepara l'utente alla sfida mentre si aspetta la parola aggiungendo i componenti grafici
     * @param friend sfidante
     */
    public void preparaSfida(String friend) {
        Main.removeAll();
        SfidaTile s=new SfidaTile(friend);
        Main.add(s);
    }

    /*                      CLASSIFICA                              */

    /**
     * Aggiorna la classifica
     * @param list lista contenente la classifica ordinata per punteggio
     */
    public void updateClassifica(List<String> list){
        //System.out.println("List Classifica: "+list.size());
        ClassificaList.setListData(list.toArray(new String[0]));
    }

    /**
     * Pulisce la classifica
     */
    public void clearClassifica(){
        ClassificaList.removeAll();
    }


}
