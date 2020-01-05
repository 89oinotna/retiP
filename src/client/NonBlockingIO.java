package client;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class NonBlockingIO implements Runnable {
    Scanner sc;

    List<String> buffer;

    public NonBlockingIO(Scanner sc) {
        this.sc = sc;
        buffer=new ArrayList<>();

    }



    @Override
    public void run(){
        String line=null;
        while(!Thread.currentThread().isInterrupted()){
            synchronized (buffer) {
                buffer.add(sc.nextLine());
                buffer.notify();

            }
            Thread.yield();


        }
    }

    public synchronized String getLine(){
        if(buffer.size()>0)
            return buffer.remove(0);
        return null;
    }
    public synchronized String getLineBlocking(){
        synchronized (buffer) {
            if (buffer.size() > 0) {

                return buffer.remove(0);
            } else {
                try {

                    buffer.wait();
                    buffer.notify();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                buffer.notify();
                return buffer.remove(0);
            }

        }

    }
}
