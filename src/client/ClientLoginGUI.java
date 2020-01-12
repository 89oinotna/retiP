package client;

import client.customComponents.FriendRequestTile;

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
                String response=c.registraUtente(nickTB.getText(), new String(pwTB.getPassword()));



            }
        });
        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String response=c.loginUtente(nickTB.getText(), new String(pwTB.getPassword()));
                System.out.println(response);
                manageResponse(response);
            }
        });
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
    }


    public void close() {
        window.dispose();
    }

    public void manageResponse(String response){
        String[] tokens=response.split(" ");
        if(tokens[0].equals("OK")){

        }
        else if(tokens[0].equals("NOK")){
            JOptionPane.showMessageDialog(window, tokens[1]);
        }
        else{

        }

    }
}
