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
 * Viene utilizzato un selector che utilizza dei worker a cui affida la gestione delle richieste
 *
 * Per permettere l'utilizzo dei worker è stata creata una lista contentente le key in uso dagli stessi
 * in modo che due worker non lavorino sulla stessa key
 *
 */
public class ServerTCP implements Runnable {
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ExecutorService executor;
    private Users users;
    private ConcurrentHashMap<SelectionKey, SelectionKey> usingK; // mi serve perchè la select potrebbe restituirmi la stessa key mentre la sto gestendo nel worker
    private final ConcurrentHashMap<String, SelectionKey> keys;
    private UDP udp;

    public ServerTCP(int port, Users _users) {
        users = _users;
        usingK=new ConcurrentHashMap<>();
        executor= Executors.newCachedThreadPool();
        keys=new ConcurrentHashMap<>();
        udp=new UDP();
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
                try {
                    if (k.isValid() && k.isAcceptable()) {
                        accept(k);
                    }
                    else if (k.isValid() && k.isReadable()) {
                        synchronized (usingK) {
                            if (usingK.putIfAbsent(k, k) == null) {
                                WorkerTCP wrk = new WorkerTCP(k, users, usingK, keys, udp, executor);
                                executor.submit(wrk);
                            }
                        }
                    }
                }catch(IOException e){
                    e.printStackTrace();
                    System.out.println("Disconnected ");
                    k.cancel();
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
