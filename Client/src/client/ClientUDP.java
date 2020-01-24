package client;

import Settings.Settings;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientUDP implements Runnable{
    private DatagramSocket socket;
    private InetAddress IPAddress;
    private ConcurrentHashMap<String, String> richiesteSfida;
    private String loggedNick;
    private String token;

    public void setLoggedInfo(String nick, String token){
        loggedNick=nick;
        this.token=token;
    }

    public ClientUDP(ConcurrentHashMap<String, String> richiesteSfida){
        this.richiesteSfida=richiesteSfida;

        try {
            IPAddress=InetAddress.getByName(Settings.HOST_NAME);
            socket = new DatagramSocket();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }



    }

    public String read() throws IOException {
        byte[] receiveData = new byte[1024];
        DatagramPacket rPacket=new DatagramPacket(receiveData, receiveData.length);

        socket.receive(rPacket);
        return new String(rPacket.getData());


    }

    public void send(String s){

        byte[] sendData=s.getBytes();
        DatagramPacket sPacket=new DatagramPacket(sendData, sendData.length, IPAddress, Settings.UDPPort);
        try {
            socket.send(sPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUdpPort(){
        return socket.getLocalPort();
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            try {
                String r=read();
                System.out.println(r);
                String[] tokens=r.split(" ");
                if(Settings.RESPONSE.valueOf(tokens[0]).equals(Settings.RESPONSE.SFIDA)){
                    //todo valida token

                    synchronized (richiesteSfida){
                        richiesteSfida.put(tokens[3], tokens[3]);
                        richiesteSfida.notify();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}


