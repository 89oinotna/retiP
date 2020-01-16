package server;

import java.nio.ByteBuffer;

public class MyAttachment{
    private String nick;
    private Integer port;
    private ByteBuffer buffer;

    MyAttachment(ByteBuffer b){
        buffer=b;
    }

    public Integer getPort() {
        return port;
    }

    public String getNick() {
        return nick;
    }

    public MyAttachment setPort(Integer port) {
        this.port=port;
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
