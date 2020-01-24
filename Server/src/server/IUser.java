package server;

import exceptions.FriendshipException;
import exceptions.UserAlreadyLogged;
import exceptions.UserNotExists;
import exceptions.WrongCredException;


import java.util.List;

public interface IUser {
    /**
     * @return true se l'utente è loggato, false altrimenti
     */
    boolean isLogged();

    /**
     * Effettua il login dell'utente
     * @param token token associato alla sessione
     * @throws UserAlreadyLogged se l'utente è già loggato
     */
    void login(String token) throws WrongCredException, UserNotExists, UserAlreadyLogged;

    /**
     * Effettua il logout dell'utente
     */
    void logout();

    boolean hasFriend(String friend);

    /**
     * Aggiunge friend alla lista amici
     * @param friend
     */
    void addFriend(String friend);

    /**
     * Rimuove friend dalla lista amici
     * @param friend
     * @return
     */
    boolean removeFriend(String friend);

    /**
     * Aggiunge friend alla pending list di amicizie
     * @param friend
     * @return true se non era presente false altrimenti
     * @throws FriendshipException se esiste già l'amicizia con friend
     */
    //todo controllare che non è già tra gli amici
    boolean addPending(String friend) throws FriendshipException;

    /**
     * Rimuove friend dalla pending list
     * @param friend
     */
    void removePending(String friend);

    /**
     * Restituisce la lista degli amici dell'utente
     * @return
     */
    List<String> getFriends();

    /**
     * Restituisce la pending list dell'utente
     * @return
     */
    List<String> getPendings();

    /**
     * Restituisce il nickname dell'utente
     * @return
     */
    String getNickname();

    /**
     * Aggiunge i punti all'utente
     *
     * @param i punti da aggiungere
     * @return punteggio aggiornato
     */
    int addScore(int i);

    /**
     *
     * @return punteggio dell'utente
     */
    int getScore();
}
