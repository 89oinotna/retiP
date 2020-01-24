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
 * Worker che si occupa della gestione di tutti i comandi TCP
 *
 * Le operazioni di inoltro vengono da un nuovo thread nella threadpool
 */
public class WriterTCP implements Runnable {

    private SelectionKey k;
    public WriterTCP(SelectionKey k) {
        this.k=k;
    }

    public void run() {
        MyAttachment att=(MyAttachment) k.attachment();
        try {

            String response=att.getResponse();
            if(response!=null)
                send(response + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            synchronized (k) {
                att.setResponse(null);
                k.interestOps(SelectionKey.OP_READ);
                k.notify();
            }
        }

    }

    /**
     * Scrive sul channell associato a k
     *
     * @param response
     * @throws IOException
     */
    public void send(String response) throws IOException {
        SocketChannel client = (SocketChannel) k.channel();

        ByteBuffer buf = ByteBuffer.wrap( response.getBytes());
        while(buf.hasRemaining()){
            client.write(buf);
        }

        /**
         * PIU LENTO MA POTREBBE ESSERE MIGLIORE PER RISPOSTE MOLTO GRANDI???
         ByteBuffer buffer = ((MyAttachment) k.attachment()).getBuffer();
         int written = 0;
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
         }*/

    }

    /**
     * Scrive sul channell associato a k
     * @param k selection key sulla quale si vuole inviare la risposta
     * @param response risposta
     * @throws IOException
     */
    public void send(SelectionKey k, String response) throws IOException {
        SocketChannel client = (SocketChannel) k.channel();

        ByteBuffer buf = ByteBuffer.wrap( response.getBytes() );

        while(buf.hasRemaining()){
            client.write(buf);
        }

        /**
         * PIU LENTO MA POTREBBE ESSERE MIGLIORE PER RISPOSTE MOLTO GRANDI???
         ByteBuffer buffer = ((MyAttachment) k.attachment()).getBuffer();
         int written = 0;
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
         }*/

    }

}
