package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.TimerTask;

public abstract class TimerChallenge extends TimerTask {

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
        while(buf.hasRemaining()){
            client.write(buf);
        }
    }
}
