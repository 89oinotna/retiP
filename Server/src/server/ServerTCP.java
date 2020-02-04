package server;

import Settings.Settings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classe che implementa la parte TCP del server
 *
 * Viene utilizzato un selector reso multithread tramite l'utilizzo di worker di lettura (ReaderTCP) e
 * scrittura (WriterTCP)
 *
 */
public class ServerTCP implements Runnable {
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ExecutorService executor;
    private ExecutorService notifierExecutor;
    private Users users;
    private final ConcurrentHashMap<String, SelectionKey> keys;
    private UDP udp;

    public ServerTCP(int port, Users _users, UDP udp) {
        users = _users;
        notifierExecutor= Executors.newCachedThreadPool();
        executor= Executors.newCachedThreadPool();
        keys=new ConcurrentHashMap<>();
        this.udp=udp;
        try {
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket(); //prendo la referencee al socket
            ss.bind(new InetSocketAddress(port)); //bindo la porta

            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT ); //registro il channel

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                selector.select(); //vedo quali sono pronti
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Set<SelectionKey> readyK = selector.selectedKeys(); //prendo i pronti
            Iterator<SelectionKey> iterator = readyK.iterator();
            while (iterator.hasNext()) {
                SelectionKey k = iterator.next();
                iterator.remove();
                synchronized (k) {
                    try {
                        if (k.isAcceptable()) {
                            accept(k);
                        } else if (k.isReadable()) {
                                k.interestOps(0);
                                ReaderTCP wrk = new ReaderTCP(k, users, keys, udp, notifierExecutor);
                                executor.submit(wrk);


                        } else if (k.isWritable()) {
                            k.interestOps(0);
                            WriterTCP wrk = new WriterTCP(k, keys, users);
                            executor.submit(wrk);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Disconnected ");
                        k.cancel();
                    }
                }
            }
        }
    }

    /**
     * Accetta le connessioni in arrivo
     * @param k selection key
     * @throws IOException
     */
    public void accept(SelectionKey k) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) k.channel();
        try {
            SocketChannel client = server.accept();
            System.out.println("Accepted connection from " + client);
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(Settings.TCPBUFFLEN);
            //todo attach nick e port
            MyAttachment att = new MyAttachment(buffer);
            SelectionKey cK = client.register(selector, SelectionKey.OP_READ, att);
        }catch(NullPointerException ignore){}
    }


}
