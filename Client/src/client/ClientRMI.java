package client;


import server.IServerRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
public class ClientRMI {
    int port;
    IServerRMI serverObject;
    Remote remoteObject;

    public ClientRMI(int port) {

        this.port = port;
        try {
            Registry r = LocateRegistry.getRegistry(port);
            remoteObject = r.lookup("GAME-SERVER");
            serverObject = (IServerRMI) remoteObject;

        } catch (Exception e) {
            System.out.println("Error in invoking object method " +
                    e.toString() + e.getMessage());
            e.printStackTrace();
        }
    }

    public String registraUtente(String nickname, String password) throws RemoteException {
            if(serverObject==null) throw new RemoteException();
            return serverObject.registraUtente(nickname, password);
    }
}
