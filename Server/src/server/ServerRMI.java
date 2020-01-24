package server;

import exceptions.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;

public class ServerRMI extends RemoteServer implements IServerRMI {
    Users users;
    int port;

    public ServerRMI(int port, Users users) {
        try {
            this.users = users;
            this.port = port;
            //porta 0 indica la porta std RMI  n
            IServerRMI stub = (IServerRMI) UnicastRemoteObject.exportObject(this, 0);
            LocateRegistry.createRegistry(port);
            Registry r = LocateRegistry.getRegistry(port);
            r.rebind("GAME-SERVER", stub);
            System.out.println("server.Server ready");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String registraUtente(String nickname, String password) throws RemoteException {
        try {
            users.registraUtente(nickname, password);
            return "OK ";
        } catch (UserAlreadyExists e) {
            //e.printStackTrace();
            return "NOK "+e.toString().split(":")[0];
        }
    }


}
