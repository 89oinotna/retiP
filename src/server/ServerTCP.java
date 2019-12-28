package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerTCP implements Runnable {
    private ServerSocketChannel serverChannel;
    //private int BUFFLEN = 1024;
    private Selector selector;
    private ExecutorService executor;
    private Utenti utenti;
    private ConcurrentHashMap<SelectionKey, SelectionKey> usingK;

    public ServerTCP(int port, Utenti _utenti) {
        try {
            utenti=_utenti;
            usingK=new ConcurrentHashMap<>();
            executor= Executors.newCachedThreadPool();
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket(); //prendo la referencee al socket
            ss.bind(new InetSocketAddress(port)); //bindo la porta
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT); //registro il channel

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            try {
                selector.selectNow(); //vedo quali sono pronti (selectNow non blocking)
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Set<SelectionKey> readyK = selector.selectedKeys(); //prendo i pronti
            Iterator<SelectionKey> iterator = readyK.iterator();
            while (iterator.hasNext()) {

                SelectionKey k = iterator.next();
                if(usingK.putIfAbsent(k,k)==null) {
                    iterator.remove();
                    WorkerTCP wrk = new WorkerTCP(selector, k, utenti, usingK);

                    executor.submit(wrk);

                }

            }
        }
    }
}
