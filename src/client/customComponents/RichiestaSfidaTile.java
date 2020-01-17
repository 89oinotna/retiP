package client.customComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class RichiestaSfidaTile extends JPanel implements CustomTile {

    private JLabel name;
    private JButton acceptBT;
    private JButton declineBT;

    public RichiestaSfidaTile(String name, ActionListener aListener, ActionListener dListener){

        this.name=new JLabel(name);
        acceptBT=new JButton();
        declineBT=new JButton();
        acceptBT.setText("V");
        declineBT.setText("X");
        acceptBT.setPreferredSize(new Dimension(50,30));
        declineBT.setPreferredSize(new Dimension(50,30));

        this.add(this.name);

        this.add(this.acceptBT);
        this.add(this.declineBT);
        this.setSize(new Dimension(this.getWidth(), acceptBT.getHeight()));
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.red),
                this.getBorder()));
        //this.setLayout(new FlowLayout(FlowLayout.RIGHT));

        acceptBT.addActionListener(aListener);
        declineBT.addActionListener(dListener);
    }

    public String getNick() {
        return name.getText();
    }
}
