package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServerRMI extends Remote {
    String registraUtente(String nickaname, String password) throws RemoteException;
}
