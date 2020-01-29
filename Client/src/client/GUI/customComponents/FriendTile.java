package client.GUI.customComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class FriendTile extends JPanel implements CustomTile {
    private JLabel name;
    private JButton sfidaBT;

    public  FriendTile(String name, ActionListener sListener){
        //this.setLayout(new FlowLayout(FlowLayout.RIGHT));
        this.name=new JLabel(name);
        sfidaBT=new JButton();
        sfidaBT.addActionListener(sListener);
        sfidaBT.setPreferredSize(new Dimension(30,30));

        this.add(this.name);

        this.add(this.sfidaBT);
    }
}
