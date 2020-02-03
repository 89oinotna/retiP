package client.GUI.customComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SfidaTile extends JPanel implements CustomTile {
    private JLabel friend;
    private JLabel parola;
    private JTextField traduzione;
    private JButton sendBT;

    public SfidaTile(String friend, String parola){
        this.friend=new JLabel(friend);
        this.parola=new JLabel(parola);
        traduzione=new JTextField();
        sendBT=new JButton();
        sendBT.setText("SEND");
        this.setLayout(new GridLayout(0,1));
        this.add(this.friend);
        this.add(this.parola);
        this.add(this.traduzione);
        this.add(this.sendBT);
    }

    public SfidaTile(String friend){
        this.friend=new JLabel(friend);
        this.parola=new JLabel();
        traduzione=new JTextField();
        sendBT=new JButton();
        sendBT.setText("SEND");
        this.setLayout(new GridLayout(0,1));
        this.add(this.friend);
        this.add(this.parola);
        this.add(this.traduzione);
        this.add(this.sendBT);



    }

    public String getTraduzione(){
        return traduzione.getText();
    }

    public void setListener(ActionListener sListener){
        sendBT.addActionListener(sListener);
    }


    public void setWord(String word) {
        parola.setText(word);
        this.updateUI();
    }
}
