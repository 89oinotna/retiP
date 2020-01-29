package server;

import exceptions.*;

import Settings.Settings;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Worker che si occupa dell'invio delle risposte al client
 *
 * Durante le operazioni del writer k.interestops()=0;
 */
public class WriterTCP implements Runnable {

    private SelectionKey k;
    private final Users users;
    private ConcurrentHashMap<String, SelectionKey> keys; //contiene le key di tutti gli utenti loggati
    public WriterTCP(SelectionKey k, ConcurrentHashMap<String, SelectionKey> keys,  Users users) {
        this.users=users;
        this.keys=keys;
        this.k=k;
    }

    public void run() {
        try {
            String response=((MyAttachment)k.attachment()).getResponse();
            if (response != null) {
                System.out.println("Response: " + response);
                send(response + "\n");
            }
            synchronized (k) {
                k.interestOps(SelectionKey.OP_READ);
                k.notify();
                k.selector().wakeup();
            }
        } catch (IOException e) {
            e.printStackTrace();
            String nick=((MyAttachment)k.attachment()).getNick();
            if(nick!=null) {
                System.out.println("Disconnected " +nick);
                logout(nick);
            }
            keys.remove(k);
            synchronized (k) {
                k.cancel();
                k.notify();
            }
        }

    }

    /**
     * Gestisce il comando di logout
     * @param nick request string tokenizzata
     */
    public void logout(String nick) {
        users.logout(nick);
        keys.remove(((MyAttachment)k.attachment()).getNick());
        k.cancel();
    }

    /**
     * Scrive sul channell associato a k
     *
     * @param response
     * @throws IOException
     */
    public void send(String response) throws IOException  {
        SocketChannel client = (SocketChannel) k.channel();

        ByteBuffer buf = ByteBuffer.wrap( response.getBytes());
        while(buf.hasRemaining()){
            client.write(buf);
        }

    }


}
