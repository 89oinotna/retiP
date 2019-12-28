package client;

import server.IServerRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class ClientRMI {
    int port;
    IServerRMI serverObject;
    Remote remoteObject;
    Scanner sc;

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

    public String registraUtente(String nickname, String password) {
        try {
            if (serverObject == null) System.out.println("bo");
            return serverObject.registraUtente(nickname, password);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }


    }
}
