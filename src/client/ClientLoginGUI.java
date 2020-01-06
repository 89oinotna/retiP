package client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class ClientLoginGUI {

    private Client c;

    private JButton loginBtn;
    private JButton registerBtn;
    private JTextField nickTB;
    private JPasswordField pwTB;
    private JPanel LoginPanel;
    private JFrame window;

    public ClientLoginGUI(Client _c) {
        c=_c;
        window = new JFrame("ClientGUI");
        window.setContentPane(this.LoginPanel);
        window.setSize(800,600);
        window.setLocation(100,100);
        window.setVisible(true);

        registerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                LoginPanel.setEnabled(false);
                System.out.println("clicked");
                String response=c.registraUtente(nickTB.getText(), Arrays.toString(pwTB.getPassword()));



            }
        });
        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String response=c.loginUtente(nickTB.getText(), Arrays.toString(pwTB.getPassword()));
            }
        });
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
    }


    public void close() {
        window.dispose();
    }
}
