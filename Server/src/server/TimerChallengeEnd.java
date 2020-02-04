package server;

import Settings.Settings;
import exceptions.UserNotExists;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Si occupa di notificare la fine della sfida alla scadenza del timer e
 * aggiornare le classifiche degli amici dei 2 partecipanti
 */
public class TimerChallengeEnd extends TimerChallenge {
    private Users users;
    private SelectionKey k1;
    private SelectionKey k2;
    private final Challenge c;
    private ConcurrentHashMap<String, SelectionKey> keys;
    TimerChallengeEnd(SelectionKey k1, SelectionKey k2, Challenge c, Users users,
                      ConcurrentHashMap<String, SelectionKey> keys, ExecutorService executor) {
        super(executor);
        this.keys=keys;
        this.k1 = k1;
        this.k2 = k2;
        this.c = c;
        this.users = users;
    }

    @Override
    public void run() {



        if (!c.endChallenge()) return;

        //se non è terminata scatta il timer
        try {
            String nick=((MyAttachment)k1.attachment()).getNick();
            String friend=((MyAttachment)k2.attachment()).getNick();
            executor.submit(new Notifier(k1, Settings.RESPONSE.SFIDA+" "+users.getToken(nick)+" "+friend+" "+Settings.SFIDA.TERMINATA+" "+c.getScore(nick)));
        } catch (UserNotExists e) {
            e.printStackTrace();
        }
        try {
            String nick=((MyAttachment)k2.attachment()).getNick();
            String friend=((MyAttachment)k1.attachment()).getNick();
            executor.submit(new Notifier(k2, Settings.RESPONSE.SFIDA+" "+users.getToken(nick)+" "+friend+" "+Settings.SFIDA.TERMINATA+" "+c.getScore(nick)));
        } catch (UserNotExists e) {
            e.printStackTrace();
        }
        try {
            aggiornaClassifiche(((MyAttachment)k1.attachment()).getNick(), ((MyAttachment)k2.attachment()).getNick());
        } catch (UserNotExists e) {
            e.printStackTrace();
        }

    }

    /**
     * Notifica a tutti gli amici l'aggiornamento della classifica
     * @param nick
     * @param friend
     * @throws UserNotExists
     */
    private void aggiornaClassifiche(String nick, String friend) throws UserNotExists {
        List<String> friends=new ArrayList<>();
        friends.addAll(users.getFriends(nick));
        friends.addAll(users.getFriends(friend));
        for (String f:friends) {
            String response=Settings.GetType.CLASSIFICA+" "+ users.getToken(f)+" "+ users.mostraClassifica(f).toJSONString();
            SelectionKey k= keys.get(f);
            if(k==null) continue ;
            executor.submit(new Notifier(k, response));
        }

    }


}
