package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServerRMI extends Remote {
    /**
     * Registrazione dell'utente su RMI
     * @param nickaname da registrare
     * @param password dell'utente che si vuole registrare
     * @return OK se viene effettuata, NOK EXCEPTION se c'è un errore
     * @throws RemoteException
     */
    String registraUtente(String nickaname, String password) throws RemoteException;
}
