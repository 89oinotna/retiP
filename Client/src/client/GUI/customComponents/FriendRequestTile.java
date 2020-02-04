package client.GUI.customComponents;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.file.Paths;

public class FriendRequestTile extends JPanel implements CustomTile {

    private JLabel name;
    private JButton acceptBT;
    private JButton declineBT;

    public FriendRequestTile(String name, ActionListener aListener, ActionListener dListener){

        this.name=new JLabel(name);
        acceptBT=new JButton();
        declineBT=new JButton();
        try {
            Image a = ImageIO.read(new FileInputStream("Client/res/plus.png"));
            a=a.getScaledInstance(25,25, Image.SCALE_SMOOTH);
            Image d = ImageIO.read(new FileInputStream("Client/res/x.png"));
            d=d.getScaledInstance(25,25, Image.SCALE_SMOOTH);

            acceptBT.setIcon(new ImageIcon(a));
            declineBT.setIcon(new ImageIcon(d));
        } catch (Exception ex) {
            System.out.println(ex);
        }

        acceptBT.setPreferredSize(new Dimension(40,40));
        declineBT.setPreferredSize(new Dimension(40,40));

        this.add(this.name);

        this.add(this.acceptBT);
        this.add(this.declineBT);
        this.setSize(new Dimension(this.getWidth(), acceptBT.getHeight()));

        //this.setLayout(new FlowLayout(FlowLayout.RIGHT));

        acceptBT.addActionListener(aListener);
        declineBT.addActionListener(dListener);
    }

    public String getNick() {
        return name.getText();
    }
}
