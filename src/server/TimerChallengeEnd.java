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

public class TimerChallengeEnd extends TimerChallenge {
    private Users users;
    private SelectionKey k1;
    private SelectionKey k2;
    private final Challenge c;

    TimerChallengeEnd(SelectionKey k1, SelectionKey k2, Challenge c, Users users) {
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
        try {
            if(!users.terminaSfida(c)) return;
        } catch (UserNotExists e) {
            return;
        }
        try {
            String nick=((MyAttachment)k1.attachment()).getNick();
            String friend=((MyAttachment)k2.attachment()).getNick();
            send(k1, Settings.RESPONSE.SFIDA+" "+users.getToken(nick)+" "+friend+" "+Settings.SFIDA.TERMINATA+" "+c.getScore(nick)+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UserNotExists e) {
            e.printStackTrace();
        }

        try {
            String nick=((MyAttachment)k2.attachment()).getNick();
            String friend=((MyAttachment)k1.attachment()).getNick();
            send(k2, Settings.RESPONSE.SFIDA+" "+users.getToken(nick)+" "+friend+" "+Settings.SFIDA.TERMINATA+" "+c.getScore(nick)+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }catch (UserNotExists e) {
            e.printStackTrace();
        }
        //todo sul client quando arriva la notifica che è terminata fa una richiesta della classifica

    }


}
