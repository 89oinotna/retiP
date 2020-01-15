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

    public ServerRMI(int _port, Users _users) {
        try {
            users = _users;
            port = _port;
            //porta 0 indica la porta std RMI  n
            IServerRMI stub = (IServerRMI) UnicastRemoteObject.exportObject(this, 0);
            LocateRegistry.createRegistry(port);
            Registry r = LocateRegistry.getRegistry(port);
            r.rebind("GAME-SERVER", stub);
            System.out.println("Server ready");

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String registraUtente(String nickname, String password) throws RemoteException {
        //return ManagerRegistrazione.registraUtente(nickaname, password);
        try {
            return "OK ";
        } catch (UserAlreadyExists e) {
            //e.printStackTrace();
            return "NOK "+e.toString().split(":")[0];
        }

        //todo gestire eccezione e ritornare valore
    }


}
