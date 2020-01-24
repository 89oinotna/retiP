package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TimerChallenge extends TimerTask {
    private final ConcurrentHashMap<SelectionKey, SelectionKey> usingK;

    protected TimerChallenge(ConcurrentHashMap<SelectionKey, SelectionKey> usingK) {
        this.usingK = usingK;
    }

    /**
     * Scrive sul channell associato a k
     *
     * @param k selection key sulla quale scrive il timer
     * @param response risposta da inviare
     * @throws IOException
     */
    public void send(SelectionKey k, String response) throws IOException {
        System.out.println("TIMER RESPONSE: "+response);
        //todo send scrittura multiplo di BUFFLEN
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buf = ByteBuffer.wrap( response.getBytes() );
        synchronized (usingK) {
            while(usingK.containsKey(k)){
                try {
                    usingK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            usingK.put(k, k);
        }
        while(buf.hasRemaining()){
            client.write(buf);
        }
        synchronized (usingK) {
            usingK.remove(k, k);
            usingK.notify();
        }
    }
}
