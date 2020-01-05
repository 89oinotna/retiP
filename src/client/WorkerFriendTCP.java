package client;

public class WorkerFriendTCP implements Runnable {
    private ClientTCP c;
    public WorkerFriendTCP(ClientTCP _c) {
        c=_c;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            String command=c.read();//AMICIZIA nick(amico) token(client)
            if(command!=null && command.length()!=0) {
                //String response = manageCommand(command); //AMICIZIA nick(client) token nick(amico) accetta/rifiuta
                //c.send(response);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
