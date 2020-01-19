package server;

import exceptions.*;
import org.json.simple.JSONArray;

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
     * @return
     */
    void logout(String nick);

    /**
     * Rimuove dalla pending list
     * Aggiunge l'amicizia tra nick e friend aggiornando la lista di amici di entrambi
     * se già c'è non fa nulla
     *
     * @param nick
     * @param friend
     */
    void aggiungiAmico(String nick, String friend) throws UserNotExists;

    /**
     * Restituisce un JSONArray con la lista degli amici
     * @param nick
     * @return lista degli amici
     */
    JSONArray listaAmici(String nick);

    /**
     * Restituisce la lista delle richieste di amicizia
     * @param nick
     * @return lista delle richieste di amicizia in sospeso
     */
    JSONArray listaRichieste(String nick);

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
     * Restituisce la classifica formata dagli amici dell'utente
     * @param nick
     * @return lista degli amici ordinati secondo il punteggio
     */
    JSONArray  mostraClassifica(String nick);

    /**
     * Restituisce il punteggio del'utente nick
     *
     * @param nick
     * @return punteggio
     */
    int mostraPunteggio(String nick);

    /**
     * Aggiorna il punteggio e restituisce il valore aggiornato
     *
     * @param nick
     * @param punteggio
     * @return punteggio aggiornato
     * @throws UserNotExists se l'utente non esiste
     */
    int aggiornaPunteggio(String nick, int punteggio) throws UserNotExists;




    /**
     * Crea la sfida tra i due users
     * @param nick
     * @param friend
     */
    Challenge sfida(String nick, String friend) throws UserNotOnline, UserAlreadyInGame, ChallengeException;


}
