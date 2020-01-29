package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public abstract class TimerChallenge extends TimerTask {
    ExecutorService executor;

    public TimerChallenge(ExecutorService executor) {
        this.executor=executor;
    }
}
