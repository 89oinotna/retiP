package server;

import Settings.Settings;

import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class UDP {
    private DatagramSocket udp;
    private InetAddress address;
    public UDP(){
        try {
            udp = new DatagramSocket(8081);

        //udp.setSoTimeout(Settings.UDP_TIMEOUT);

            address = InetAddress.getByName(Settings.HOST_NAME);

        }catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void write(String nick, String friend, String friendToken, int port) throws IOException {
        String msg = Settings.RESPONSE.SFIDA+" "+friend+" "+
                friendToken+" "+nick+" \n";
        byte[] msgSfida = msg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(msgSfida, msgSfida.length, address,
                port);
        udp.send(packet);
    }




}
