package client;

import java.io.IOException;
import java.net.*;

public class ClientUDP {
    private DatagramSocket cs;
    private int port=8081;
    private InetAddress IPAddress;
    public ClientUDP(){

        try{
            IPAddress = InetAddress.getByName("localhost");
            cs=new DatagramSocket();
            //cs.setSoTimeout(1000);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void read() throws IOException {
        byte[] receiveData = new byte[1024];
        DatagramPacket rPacket=new DatagramPacket(receiveData, receiveData.length);
        try{
            cs.receive(rPacket);
            String response = new String(rPacket.getData());


        }catch (SocketTimeoutException e){
            e.printStackTrace();
        }
    }

    public void send(String s){

        byte[] sendData=s.getBytes();
        DatagramPacket sPacket=new DatagramPacket(sendData, sendData.length, IPAddress, port);
        try {
            cs.send(sPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUdpPort(){
        return cs.getLocalPort();
    }
}
