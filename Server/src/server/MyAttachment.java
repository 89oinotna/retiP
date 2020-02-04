package server;

import java.nio.ByteBuffer;

/**
 * Oggetto usato come attachment per le SelectioKey contenente le informazioni sull'utente loggato su quella key
 */
public class MyAttachment{
    private String nick;
    private String token;
    private Integer UDPport;
    private ByteBuffer buffer;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    private String response;
    MyAttachment(ByteBuffer b){
        buffer=b;
        response=null;
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

    public MyAttachment setToken(String token){
        this.token=token;
        return this;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public String getToken() {
        return token;
    }
}
