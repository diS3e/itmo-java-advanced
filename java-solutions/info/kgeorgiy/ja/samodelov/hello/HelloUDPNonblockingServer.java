package info.kgeorgiy.ja.samodelov.hello;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer extends HelloUDPServer {

    private Selector selector;
    private DatagramChannel datagramChannel;
    private final Deque<ResponseData> responses = new ConcurrentLinkedDeque<>();

    @Override
    public void start(int port, int threads) {
        try {
            selector = Selector.open();
            threadPool = Executors.newFixedThreadPool(threads);
            datagramChannel = (DatagramChannel) DatagramChannel.open()
                    .bind(new InetSocketAddress(port))
                    .configureBlocking(false)
                    .register(selector, SelectionKey.OP_READ)
                    .channel();
            serverThread = Executors.newSingleThreadExecutor();
            serverThread.submit(this::runBlockingServer);

        } catch (IOException e) {
            System.err.println("Can't start server, I/O error occurs");
            close();
        }
    }

    private void runBlockingServer() {
        while (!datagramChannel.socket().isClosed() && !Thread.interrupted()) {
            try {
                if (selector.select() == 0) continue;
            } catch (IOException e) {
                close();
            }
            for (var it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                final SelectionKey key = it.next();
                try {
                    if (key.isValid()) {
                        if (key.isReadable()) {
                            read(key);
                        } else if (key.isWritable()) {
                            write(key);
                            key.interestOpsAnd(~SelectionKey.OP_READ);
                        } else {
                            close();
                            throw new RuntimeException("Key was broken");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("IOException handled");
                } finally {
                    it.remove();
                }
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize());
        SocketAddress address = datagramChannel.receive(buffer);
        threadPool.submit(() -> {
            responses.add(new ResponseData(address,
                    Utils.response(Utils.CHARSET.decode(buffer.flip()).toString())));
            key.interestOps(SelectionKey.OP_WRITE);
            selector.wakeup();
        });
    }

    private void write(SelectionKey key) throws IOException {
        if (!responses.isEmpty()) {
            ResponseData metaInfo = responses.poll();
            datagramChannel.send(ByteBuffer.wrap(metaInfo.response.getBytes(Utils.CHARSET)), metaInfo.socket);
        }
    }

    @Override
    public void close() {
        try {
            datagramChannel.close();
            selector.close();
            threadPool.close();
            serverThread.close();
        } catch (IOException e) {
            System.err.println("Closing throw IOException");
        }
    }

    private record ResponseData(SocketAddress socket, String response) {
    }
}
