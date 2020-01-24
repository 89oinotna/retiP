package server;

import Settings.Settings;
import exceptions.UserNotExists;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Si occupa di notificare la scadenza del timer della richiesta di sfida
 */
public class TimerChallengeRequest extends TimerChallenge {
    private Users users;
    private SelectionKey k1;
    private SelectionKey k2;
    private String nick;
    private String friend;

    //friend è l'amico con la entry nella lista delle challengRequest
    public TimerChallengeRequest(String nick, String friend, SelectionKey k1, SelectionKey k2, Users users,
                                 ConcurrentHashMap<SelectionKey, SelectionKey> usingK) {
        super(usingK);
        this.nick=nick;
        this.friend=friend;
        this.k1 = k1;
        this.k2=k2;
        this.users = users;
    }

    @Override
    public void run() {
        synchronized (users){
            try {
                if(!users.removePendingChallenge(nick, friend)) {
                    return;
                }
            } catch (UserNotExists ignored) {
            }
        }

        try {
            String nick=((MyAttachment)k2.attachment()).getNick();
            String friend=((MyAttachment)k1.attachment()).getNick();
            send(k2, Settings.RESPONSE.SFIDA+" "+users.getToken(nick)+" "+friend+" "+Settings.SFIDA.SCADUTA+"\n");
        } catch (IOException | UserNotExists e) {
            e.printStackTrace();
        }

    }
}
