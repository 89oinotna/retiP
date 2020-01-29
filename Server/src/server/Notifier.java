package server;

import java.nio.channels.SelectionKey;

/**
 * Thread che permette di mettersi in attesa di una key e impostarla in write con una risposta quando non in uso (READ)
 */
public class Notifier implements  Runnable {
    private SelectionKey k;
    private String response;
    public Notifier(SelectionKey k, String response){
        this.k=k;
        this.response=response;
    }

    @Override
    public void run() {
        synchronized (k){
            while(k.isValid() && k.interestOps()!= SelectionKey.OP_READ){
                try {
                    k.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(k.isValid()) {
                k.interestOps(SelectionKey.OP_WRITE);
                ((MyAttachment)k.attachment()).setResponse(response);
                k.selector().wakeup();
            }
        }
    }
}
