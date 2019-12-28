package server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;


public class conn {
    private int BUFFSIZE = 1024;
    ByteBuffer buf;
    SocketAddress sa;

    public conn() {
        buf = ByteBuffer.allocate(BUFFSIZE);
    }
}