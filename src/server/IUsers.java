package server;

import exceptions.*;

import java.util.List;

public interface IUsers {

    /**
     * Effettua la registrazione dell'utente
     *
     * @param nick
     * @param pw
     * @throws UserAlreadyExists se l'utente è già registrato
     */
    void registraUtente(String nick, String pw) throws UserAlreadyExists;

    /**
     * Effettua il login dell'utente
     *
     * @param nick
     * @param pw
     * @return token che identifica la sessione
     */
    String login(String nick, String pw)throws WrongCredException, UserNotExists, UserAlreadyLogged;

    /**
     * Effettua il logout dell'utente
     * @param nick
     */
    void logout(String nick);

    /**
     * Controlla se l'utente è online
     * @param nick
     * @return true se è online, false altrimenti
     */
    boolean isLogged(String nick);

    /**
     * Controlla l'esistenza di un utente
     * @param nick
     * @return true se esiste, flase altrimenti
     */
    boolean exists(String nick);

    /**
     * Rimuove dalla pending list
     * Aggiunge l'amicizia tra nick e friend aggiornando la lista di amici di entrambi
     * se già c'è non fa nulla
     *  @param nick
     * @param friend
     * @return true se l'amiciz a non era già presente, false altrimenti
     */
    boolean aggiungiAmico(String nick, String friend) throws UserNotExists;

    /**
     * Controlla che l'utente nick ha come amico friend
     * @param nick
     * @param friend
     * @return true se esiste l'amicizia, flase altrimenti
     */
    boolean hasFriend(String nick, String friend);

    /**
     * Restituisce la lista degli amici
     * @param nick
     * @return lista degli amici
     */
    List<String> getFriends(String nick) throws UserNotExists;

    /**
     * Restituisce la lista delle richieste di amicizia
     * @param nick
     * @return lista delle richieste di amicizia in sospeso
     */
    List<String> getPendings(String nick);

    /**
     * Aggiunge una richiesta di amicizia
     * @param nick che richiede l'amicizia
     * @param friend
     * @throws FriendshipException se esiste già l'amicizia tra i due
     * @return true se non era presente false altrimenti
     */
    boolean addPending(String nick, String friend) throws FriendshipException;

    /**
     * Rimuove la richiesta di amicizia
     * @param nick da cui rimuovere la richiesta
     * @param friend che aveva richiesto l'amicizia
     */
    void removePending(String nick, String friend);

    /**
     * Controlla che nick abbia la richiesta di amicizia da friend
     * @param nick
     * @param friend
     * @return true se ce l'ha, false altrimenti
     */
    boolean containsPending(String nick, String friend);

    /**
     * Restituisce la classifica formata dagli amici dell'utente
     * @param nick
     * @return lista degli amici ordinati secondo il punteggio
     */
    List<IUser> getLeaderboard(String nick);

    /**
     * Restituisce il punteggio del'utente nick
     *
     * @param nick
     * @return punteggio
     */
    int mostraPunteggio(String nick);




}
