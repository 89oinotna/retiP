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

/**
 * Classe udp che si occupa dell'inoltro delle richieste di sfida
 */
public class UDP {
    private DatagramSocket udp;
    private InetAddress address;
    public UDP(){
        try {
            udp = new DatagramSocket(8081);
            address = InetAddress.getByName(Settings.HOST_NAME);

        }catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Invio della richiesta di sfida
     * @param nick from
     * @param friend to
     * @param friendToken token di sessione
     * @param port porta udp di friend
     * @throws IOException
     */
    public void write(String nick, String friend, String friendToken, int port) throws IOException {
        String msg = Settings.RESPONSE.SFIDA+" "+friend+" "+
                friendToken+" "+nick+" \n";
        byte[] msgSfida = msg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(msgSfida, msgSfida.length, address,
                port);
        udp.send(packet);
    }

}
