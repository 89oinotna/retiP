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
     * @param k
     * @param response
     * @throws IOException
     */
    public void send(SelectionKey k, String response) throws IOException {
        System.out.println("TIMER RESPONSE: "+response);
        //todo send scrittura multiplo di BUFFLEN
        SocketChannel client = (SocketChannel) k.channel();
        ByteBuffer buffer = ((MyAttachment) k.attachment()).getBuffer();
        int written = 0;
        //todo align response to kBUFFLEN
        response.length();
        byte[] b = response.getBytes();
        while ((b.length - written) > 0) { //ciclo fino a che non ho scritto tutto
            if ((b.length - written) % buffer.capacity() != 0)
                buffer.put(Arrays.copyOfRange(b, written, (written) + ((b.length - written) % buffer.capacity())));
            else buffer.put(Arrays.copyOfRange(b, written, (written) + buffer.capacity())); //copio una parte


            buffer.flip();
            int w = 0;
            while (w < buffer.limit()) {
                //System.out.println(new String(buffer.array()));
                w = client.write(buffer);

                written += w;
                //System.out.println(w);
            }

            buffer.clear();
        }
    }
}
