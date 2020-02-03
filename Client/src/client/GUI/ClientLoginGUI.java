package client.GUI;



import Settings.Settings;
import client.ClientRMI;
import client.ClientTCP;
import client.ClientUDP;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;

public class ClientLoginGUI {
    private JButton loginBtn;
    private JButton registerBtn;
    private JTextField nickTB;
    private JPasswordField pwTB;
    private JPanel LoginPanel;
    private JFrame window;
    private ClientRMI rmi;
    private ClientTCP tcp;
    private ClientUDP udp;

    public ClientLoginGUI(ClientRMI rmi, ClientTCP tcp, ClientUDP udp) {
        this.rmi = rmi;
        this.tcp = tcp;
        this.udp = udp;
        window = new JFrame("ClientGUI");
        window.setContentPane(this.LoginPanel);
        window.setSize(800, 600);
        window.setLocation(100, 100);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        registerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    registraUtente(nickTB.getText(), new String(pwTB.getPassword()));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");
                }


            }
        });
        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    loginUtente(nickTB.getText(), new String(pwTB.getPassword()));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(window, "SERVER DISCONNESSO");

                }
            }
        });
    }

    public void close() {
        window.dispose();
    }

    /**
     * Registrazione utente tramite RMI
     *
     * @param nick
     * @param pw
     * @throws IOException
     */
    public void registraUtente(String nick, String pw) throws IOException {
        String response;
        try {
            response = rmi.registraUtente(nick, pw);
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(window, e.toString().split(" ")[0]);
            return;
        }
        System.out.print(response);
        String[] tokens = response.split(" ");

        if (Settings.RESPONSE.NOK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
            JOptionPane.showMessageDialog(window, tokens[1]);
        } else if (Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
            loginUtente(nick, pw);
        } else {
            JOptionPane.showMessageDialog(window, new IllegalArgumentException().toString().split(" ")[0]);
        }

    }

    /**
     * Login utente con tcp
     *
     * @param nick
     * @param pw
     * @return
     * @throws IOException
     */
    public String loginUtente(String nick, String pw) throws IOException {
        String response = tcp.login(nick, pw, udp.getUdpPort());
        String[] tokens = response.split(" ");
        if (Settings.RESPONSE.NOK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
            JOptionPane.showMessageDialog(window, tokens[1]);
            return response;
        } else if (Settings.RESPONSE.OK.equals(Settings.RESPONSE.valueOf(tokens[0]))) {
            udp.setLoggedInfo(nick, tokens[2]);
            synchronized (tcp) {
                tcp.notify();
            }
            return response;
        }
        JOptionPane.showMessageDialog(window, new IllegalArgumentException().toString().split(" ")[0]);
        return null;
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
        LoginPanel = new JPanel();
        LoginPanel.setLayout(new GridBagLayout());
        pwTB = new JPasswordField();
        pwTB.setEditable(true);
        pwTB.setEnabled(true);
        pwTB.setPreferredSize(new Dimension(120, 30));
        pwTB.setText("");
        pwTB.setToolTipText("Password");
        pwTB.putClientProperty("html.disable", Boolean.FALSE);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        LoginPanel.add(pwTB, gbc);
        registerBtn = new JButton();
        registerBtn.setPreferredSize(new Dimension(120, 30));
        registerBtn.setText("Register");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        LoginPanel.add(registerBtn, gbc);
        loginBtn = new JButton();
        loginBtn.setEnabled(true);
        loginBtn.setPreferredSize(new Dimension(120, 30));
        loginBtn.setText("Login");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        LoginPanel.add(loginBtn, gbc);
        nickTB = new JTextField();
        nickTB.setHorizontalAlignment(2);
        nickTB.setPreferredSize(new Dimension(120, 30));
        nickTB.setText("");
        nickTB.setToolTipText("Username");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        LoginPanel.add(nickTB, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return LoginPanel;
    }
}
