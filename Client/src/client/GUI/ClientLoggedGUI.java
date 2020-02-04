package client.GUI;

import Settings.Settings;
import client.ClientTCP;
import client.GUI.customComponents.*;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
    private Thread amiciziaTH;
    private Thread pendingTH;
    private Thread classificaTH;
    private Thread richiesteSfidaTH;
    private Thread sfidaTH;
    private List<String> sfida;
    private List<String> pendingFriendsList;
    private List<String> friendsList;
    private List<String> classificaList;
    private ConcurrentHashMap<String, String> richiesteSfida;

    public ClientLoggedGUI(ClientTCP tcp, String nick,
                           List<String> sfida, List<String> pendingFriendsList,
                           List<String> friendsList, List<String> classificaList,
                           ConcurrentHashMap<String, String> richiesteSfida) {

        this.tcp = tcp;
        this.sfida = sfida;
        this.pendingFriendsList = pendingFriendsList;
        this.friendsList = friendsList;
        this.classificaList = classificaList;
        this.richiesteSfida = richiesteSfida;
        window = new JFrame("ClientLoggedGUI");
        window.setContentPane(this.LoggedPanel);
        window.setSize(800, 600);
        Menu.setPreferredSize(new Dimension((int) (window.getWidth() * 0.3), Menu.getHeight()));
        Main.setPreferredSize(new Dimension((int) (window.getWidth() * 0.3), Main.getHeight()));
        Main.setLayout(new GridLayout(1, 1));
        Friend.setPreferredSize(new Dimension((int) (window.getWidth() * 0.3), Friend.getHeight()));
        Classifica.setPreferredSize(new Dimension(Classifica.getWidth(), (int) (Menu.getHeight() * 0.2)));
        SfideScroll.setPreferredSize(new Dimension(SfideScroll.getWidth(), (int) (Menu.getHeight() * 0.2)));
        pendingRequest.setPreferredSize(new Dimension(pendingRequest.getWidth(), (int) (Friend.getHeight() * 0.2)));
        friendsScroll.setPreferredSize(new Dimension(friendsScroll.getWidth(), (int) (Friend.getHeight() * 0.2)));
        pending.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        friends.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        Sfide.setLayout(new VerticalFlowLayout(VerticalFlowLayout.RIGHT, VerticalFlowLayout.TOP));
        window.setLocation(100, 100);
        window.setVisible(true);
        Name.setText(nick);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //start listeners
        startListeners();

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
                } catch (IOException ex) {
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
     * Avvia tutti i thread listener sulle strutture dati che serviranno per effettuare
     * l'aggiornamento della GUI
     */
    private void startListeners() {
        amiciziaTH = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (friendsList) {
                        //todo aggiungere flag online?
                        try {
                            friendsList.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        clearFriend();
                        for (String s : friendsList) {
                            addFriendTile(s);
                        }
                        friends.revalidate();
                        friends.repaint();
                    }
                }
            }
        });
        pendingTH = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (pendingFriendsList) {
                        try {
                            pendingFriendsList.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        clearPending();
                        for (String s : pendingFriendsList) {
                            addPendingFriendTile(s);
                        }
                        pending.revalidate();
                        pending.repaint();
                    }
                }
            }
        });
        classificaTH = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (classificaList) {
                        try {
                            classificaList.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        clearClassifica();
                        updateClassifica(classificaList);
                        ClassificaList.revalidate();
                        ClassificaList.repaint();
                    }
                }
            }
        });
        richiesteSfidaTH = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (richiesteSfida) {
                        try {
                            richiesteSfida.wait();
                            clearRichiesteSfida();
                            for (String nick : richiesteSfida.keySet()) {
                                addSfidaTile(nick);
                            }
                            Sfide.revalidate();
                            Sfide.repaint();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });
        sfidaTH = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        synchronized (sfida) {
                            while (sfida.isEmpty()) {
                                sfida.wait();
                            }
                            if (sfida.size() == 1) {
                                try {
                                    int s = Integer.parseInt(sfida.get(0));
                                    endSfida(s);
                                } catch (NumberFormatException e) {
                                    preparaSfida(sfida.get(0));
                                }
                                sfida.clear();
                            } else if (sfida.size() == 2) {
                                initSfida(sfida.get(0), sfida.get(1));
                                sfida.clear();
                            }
                            Main.revalidate();
                            Main.repaint();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        amiciziaTH.start();
        pendingTH.start();
        classificaTH.start();
        richiesteSfidaTH.start();
        sfidaTH.start();
    }

    /**
     * Chiude la finestra e termina i thread per le strutture condivise
     */
    public void close() {
        amiciziaTH.interrupt();
        pendingTH.interrupt();
        classificaTH.interrupt();
        richiesteSfidaTH.interrupt();
        sfidaTH.interrupt();
        window.dispose();
    }

    /**
     * Effettua il logout resettando tcp
     */
    private void logout() {
        synchronized (tcp) {
            tcp.setLoggedNick(null);
            tcp.setToken(null);
            tcp.notify();
        }
    }

    /*                      RICHIESTE AMICIZIA                      */

    /**
     * Aggiunge alla lista delle richieste ricevute
     *
     * @param friend da chi viene la richiesta
     */
    public void addPendingFriendTile(String friend) {
        //listener bottone che accetta
        ActionListener aListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    tcp.accettaAmico(friend);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }
            }
        };

        //listener per bottone che rifiuta
        ActionListener dListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    tcp.rifiutaAmico(friend);
                } catch (IOException ex) {
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
    public void clearPending() {
        pending.removeAll();
    }

    /*                      AMICI                                   */

    /**
     * Aggiunge amico alla lista delle amicizie
     *
     * @param friend amico da inserire
     */
    public void addFriendTile(String friend) {
        //listener per bottone sfida
        ActionListener sListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    String[] tokens = tcp.inviaSfida(friend).split(" ");
                    if (Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
                        JOptionPane.showMessageDialog(window, tokens[0]);
                    } else if (Settings.RESPONSE.NOK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
                        JOptionPane.showMessageDialog(window, tokens[1]);
                    }

                } catch (IOException ex) {
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
    public void clearFriend() {
        friends.removeAll();
    }

    /*                      SFIDA                                   */

    /**
     * Aggiunge una richiesta diu sfida
     *
     * @param friend amico che richiede la sfida
     */
    public void addSfidaTile(String friend) {
        ActionListener aListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {

                    String[] tokens = tcp.accettaSfida(friend).split(" ");
                    if (Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
                        preparaSfida(friend);

                    } else if (Settings.RESPONSE.NOK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
                        JOptionPane.showMessageDialog(window, tokens[1]);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                    logout();
                }
            }
        };
        ActionListener rListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    tcp.rifiutaSfida(friend);
                } catch (IOException ex) {
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
     *
     * @param friend sfidante
     * @param parola parola da tradurre
     */
    public void initSfida(String friend, String parola) {
        Main.removeAll();
        SfidaTile s = new SfidaTile(friend, parola);
        ActionListener sListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String r = tcp.inviaTraduzione(s.getTraduzione());
                    try {
                        Integer.parseInt(r);

                    } catch (NumberFormatException ex) {
                        s.setWord(r);
                    }

                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(window, ex.getMessage());
                } catch (IOException ex) {
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
     *
     * @param score punteggio
     */
    public void endSfida(int score) {
        JOptionPane.showMessageDialog(window, "HAI TOTALIZZATO: " + score + " PUNTI");
        Main.removeAll();
    }

    /**
     * Prepara l'utente alla sfida mentre si aspetta la parola aggiungendo i componenti grafici
     *
     * @param friend sfidante
     */
    public void preparaSfida(String friend) {
        Main.removeAll();
        SfidaTile s = new SfidaTile(friend);
        Main.add(s);
        Main.revalidate();
        Main.repaint();
    }

    /*                      CLASSIFICA                              */

    /**
     * Aggiorna la classifica
     *
     * @param list lista contenente la classifica ordinata per punteggio
     */
    public void updateClassifica(List<String> list) {
        //System.out.println("List Classifica: "+list.size());
        ClassificaList.setListData(list.toArray(new String[0]));
    }

    /**
     * Pulisce la classifica
     */
    public void clearClassifica() {
        ClassificaList.removeAll();
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        LoggedPanel = new JPanel();
        LoggedPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        Friend = new JPanel();
        Friend.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        LoggedPanel.add(Friend, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pendingRequest = new JScrollPane();
        pendingRequest.setHorizontalScrollBarPolicy(31);
        pendingRequest.setVerticalScrollBarPolicy(20);
        Friend.add(pendingRequest, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        pending = new JPanel();
        pending.setLayout(new GridBagLayout());
        pending.setEnabled(false);
        pending.setInheritsPopupMenu(false);
        pending.setMaximumSize(new Dimension(2147483647, 2147483647));
        pending.setMinimumSize(new Dimension(100, 200));
        pending.setName("");
        pending.setToolTipText("Friend Request");
        pendingRequest.setViewportView(pending);
        friendsScroll = new JScrollPane();
        Friend.add(friendsScroll, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        friends = new JPanel();
        friends.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        friends.setToolTipText("Friends");
        friendsScroll.setViewportView(friends);
        AddFriendP = new JPanel();
        AddFriendP.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        Friend.add(AddFriendP, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addFriendTB = new JTextField();
        AddFriendP.add(addFriendTB, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addBT = new JButton();
        addBT.setText("Add Friend");
        AddFriendP.add(addBT, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pendingLB = new JLabel();
        pendingLB.setText("Richieste di Amicizia");
        Friend.add(pendingLB, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        friendsLB = new JLabel();
        friendsLB.setText("Amici");
        Friend.add(friendsLB, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Main = new JPanel();
        Main.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        LoggedPanel.add(Main, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Menu = new JPanel();
        Menu.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        LoggedPanel.add(Menu, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        Name = new JLabel();
        Name.setText("Label");
        Menu.add(Name, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logoutBT = new JButton();
        logoutBT.setText("Logout");
        Menu.add(logoutBT, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        Classifica = new JScrollPane();
        Menu.add(Classifica, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        ClassificaList = new JList();
        ClassificaList.setToolTipText("Leaderboard");
        Classifica.setViewportView(ClassificaList);
        SfideScroll = new JScrollPane();
        Menu.add(SfideScroll, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        Sfide = new JPanel();
        Sfide.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        Sfide.setToolTipText("Challenge Request");
        SfideScroll.setViewportView(Sfide);
        challengeLB = new JLabel();
        challengeLB.setText("Richieste di Sfida");
        Menu.add(challengeLB, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        leaderboardLB = new JLabel();
        leaderboardLB.setText("Classifica");
        Menu.add(leaderboardLB, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return LoggedPanel;
    }

}
