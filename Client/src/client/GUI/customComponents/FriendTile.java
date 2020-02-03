package client.GUI.customComponents;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.FileInputStream;

public class FriendTile extends JPanel implements CustomTile {
    private JLabel name;
    private JButton sfidaBT;

    public  FriendTile(String name, ActionListener sListener){
        //this.setLayout(new FlowLayout(FlowLayout.RIGHT));
        this.name=new JLabel(name);
        sfidaBT=new JButton();
        sfidaBT.addActionListener(sListener);
        sfidaBT.setPreferredSize(new Dimension(40,40));
        try {
            Image s = ImageIO.read(new FileInputStream("Client/res/sfida.png"));
            s=s.getScaledInstance(25,25, Image.SCALE_SMOOTH);
            sfidaBT.setIcon(new ImageIcon(s));
        } catch (Exception ex) {
            System.out.println(ex);
        }

        this.add(this.name);

        this.add(this.sfidaBT);
    }
}
