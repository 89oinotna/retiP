package client;

import client.customComponents.CustomTile;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class FriendListener<E extends JPanel & CustomTile> implements OnEventListener {

    private List<String> list;
    private Class<E> clazz;
    public FriendListener(List<String> list, Class<E> clazz){
        this.list=list;
        this.clazz=clazz;

    }


    @Override
    public void onEvent(JScrollPane pane, String name) {
        pane.add(createContents(name));
    }

    E createContents(String name) {
        try {
            return clazz.getDeclaredConstructor().newInstance(name);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }
}
