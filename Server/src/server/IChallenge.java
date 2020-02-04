package server;

import exceptions.ChallengeException;
import exceptions.UserNotExists;

public abstract class IChallenge {

    /**
     * Controlla se la sfida è attiva
     * @return true se è attiva, false altrimenti
     */
    abstract boolean isActive();

    /**
     * Imposta la sfida come terminata aggiornando i punteggi
     * In caso di pareggio nessuno riceve i punti extra
     * @return true se la termina, false se già lo era
     */
    abstract boolean endChallenge();

    /**
     * Restituisce il punteggio corrente di nick nella partita
     * @param nick user
     * @return punteggio di nick
     * @throws UserNotExists se nick non è utente della partita
     */
    abstract int getScore(String nick) throws UserNotExists;

    /**
     * Dato il nick restituisce il nome dell'avversario
     * @param nick user
     * @return nome dell'avversario
     * @throws ChallengeException se nick non è presente nella partita
     */
    abstract String getOpponent(String nick) throws ChallengeException;
}
