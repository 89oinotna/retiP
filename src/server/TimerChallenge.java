package server;

import Settings.Settings;
import exceptions.UserNotExists;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class TimerChallenge extends TimerTask {
    private Users users;
    private SelectionKey k1;
    private SelectionKey k2;
    private final Challenge c;

    TimerChallenge(SelectionKey k1, SelectionKey k2, Challenge c, Users users) {
        //todo usingK
        this.k1 = k1;
        this.k2 = k2;
        this.c = c;
        this.users = users;
    }

    @Override
    public void run() {
        //System.out.println("TIMERTASK");
        //se nessuno è arrivato alla fine scatta il timer
        synchronized (c) {
            if (c.isActive()) {
                c.endChallenge();
            }
            else return;
        }
        try {
            String nick=((MyAttachment)k1.attachment()).getNick();
            String friend=((MyAttachment)k2.attachment()).getNick();
            send(k1, Settings.RESPONSE.SFIDA+" "+users.getToken(nick)+" "+friend+" "+Settings.SFIDA.TERMINATA+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UserNotExists e) {
            e.printStackTrace();
        }

        try {
            String nick=((MyAttachment)k2.attachment()).getNick();
            String friend=((MyAttachment)k1.attachment()).getNick();
            send(k2, Settings.RESPONSE.SFIDA+" "+users.getToken(nick)+" "+friend+" "+Settings.SFIDA.TERMINATA+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }catch (UserNotExists e) {
            e.printStackTrace();
        }
        //todo sul client quando arriva la notifica che è terminata fa una richiesta della classifica

    }


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
