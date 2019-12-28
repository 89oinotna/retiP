package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class ClientTCP {
    private SocketAddress address;
    private SocketChannel socketChannel;
    private ByteBuffer byteBuffer;
    private int BUFFLEN=1024;

    public ClientTCP(){
        address = new InetSocketAddress("127.0.0.1", 8080);


        try {
            socketChannel = SocketChannel.open();
            //socketChannel.configureBlocking(false);
            socketChannel.connect(address);
            socketChannel.configureBlocking(false);
            byteBuffer=ByteBuffer.allocate(BUFFLEN);
            while (!socketChannel.finishConnect()) {
                //System.out.println("Non terminata la connessione");
            }
            System.out.println("Terminata la connessione");



        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    }

    public String read(){
        String msg = "";

        int read = 0;
        try {
            read = socketChannel.read(byteBuffer);

        while (read > 0) {

            String ne=new String(Arrays.copyOfRange(byteBuffer.array(), 0, read));
            msg += ne;
            byteBuffer.clear();
            System.out.println(ne+read);
            read = socketChannel.read(byteBuffer);
            //
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }

    public void send(String message){
        //todo aggiusta sta merda
        ByteBuffer buf = ByteBuffer.wrap(message.getBytes());
        System.out.println(message);
        try {
            while (buf.hasRemaining()) {

                int w=socketChannel.write(buf);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buf.clear();
    }

    /**
     * effettua il login su tCP
     * @param nick
     * @param pw
     * @return il token per la sessione
     */
    public String login(String nick, String pw){
        String message="LOGIN "+nick+" "+pw;
        send(message);
        String response=read();
        while(response==null||response.length()==0){
            response=read();
        }
        return response;
    }
}

