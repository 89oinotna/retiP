package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

public class ServerUdp implements Runnable {
    private Selector selector;

    private static void read(SelectionKey key) throws IOException {
        DatagramChannel ch = (DatagramChannel) key.channel();
        ((DatagramChannel) key.channel()).getRemoteAddress();
        conn c = (conn) key.attachment();
        c.sa = ch.receive(c.buf);
        System.out.print(c.sa.toString() + "> " + new String(c.buf.array(), "UTF-8").trim());
        c.buf.flip();


    }

    private static void write(SelectionKey key) throws IOException, InterruptedException {
        DatagramChannel ch = (DatagramChannel) key.channel();
        conn c = (conn) key.attachment();
        System.out.print(" ACTION: ");
        if (Math.random() * 100 < 25) {
            System.out.println("not sent");
            return;
        }
        long delay = (long) (Math.random() * 500);
        System.out.println("delayed " + delay);
        Thread.sleep(delay);
        int sent = ch.send(c.buf, c.sa);
        c.buf.clear();
    }

    public ServerUdp() {
        try {
            selector = Selector.open();
            DatagramChannel dc = DatagramChannel.open();
    //dc.tim
            dc.socket().bind(new InetSocketAddress(8081));
            dc.configureBlocking(false);
            SelectionKey clientK = dc.register(selector, SelectionKey.OP_READ);
            clientK.attach(new conn());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void accept(SelectionKey k){


    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator selectedK = selector.selectedKeys().iterator();
                while (selectedK.hasNext()) {
                    try {
                        SelectionKey key = (SelectionKey) selectedK.next();
                        selectedK.remove();
                        if (key.isValid()) {
                            if(key.isAcceptable()){
                                accept(key);
                            }
                            if (key.isReadable()) {
                                read(key);
                                key.interestOps(SelectionKey.OP_WRITE);
                            } else if (key.isWritable()) {
                                write(key);
                                key.interestOps(SelectionKey.OP_READ);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
