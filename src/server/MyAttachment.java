package server;

import java.nio.ByteBuffer;

public class MyAttachment{
    private String nick;
    private Integer UDPport;
    private ByteBuffer buffer;

    MyAttachment(ByteBuffer b){
        buffer=b;
    }

    public Integer getUDPPort(){
        return UDPport;
    }

    public String getNick() {
        return nick;
    }

    public MyAttachment setUDPPort(Integer port) {
        this.UDPport=port;
        return this;
    }

    public MyAttachment setNick(String nick) {
        this.nick=nick;
        return this;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
