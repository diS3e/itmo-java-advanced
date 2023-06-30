package info.kgeorgiy.ja.samodelov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.stream.IntStream;


public class HelloUDPNonblockingClient extends HelloUDPClient {
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
        ArrayList<Integer> requestsInChannel = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            requestsInChannel.add(1);
        }

        try (Selector selector = Selector.open()) {
            IntStream.range(1, threads + 1)
                    .forEach(i -> {
                        try {
                            DatagramChannel.open()
                                    .connect(inetSocketAddress)
                                    .configureBlocking(false)
                                    .register(selector, SelectionKey.OP_WRITE, i);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            while (!selector.keys().isEmpty()) {
                if (selector.select(Utils.TIMEOUT) == 0)
                    selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                for (final var it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    final SelectionKey key = it.next();
                    try {
                        if (key.isValid()) {
                            final DatagramChannel datagramChannel = (DatagramChannel) key.channel();
                            final int channelId = (Integer) key.attachment();
                            final int requestId = requestsInChannel.get(channelId - 1);
                            if (key.isReadable()) {
                                ByteBuffer byteBuffer = ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize());
                                datagramChannel.receive(byteBuffer);
                                String response = Utils.CHARSET.decode(byteBuffer.flip()).toString();
                                if (Utils.isCorrectResponse(response, channelId, requestId)) {
                                    System.out.println("Received: " + response);
                                    requestsInChannel.set(channelId - 1, requestId + 1);
                                }
                                key.interestOps(SelectionKey.OP_WRITE);
                                if (requestsInChannel.get(channelId - 1) > requests) {
                                    datagramChannel.close();
                                }
                            } else if (key.isWritable()) {
                                String message = Utils.createMessage(prefix, channelId, requestId);
                                datagramChannel.send(ByteBuffer.wrap(message.getBytes(Utils.CHARSET)), inetSocketAddress);
                                key.interestOps(SelectionKey.OP_READ);
                            } else {
                                throw new RuntimeException("Key was broken: " + key.readyOps());
                            }
                        }
                    } finally {
                        it.remove();
                    }
                }
            }

        } catch (IOException | UncheckedIOException e) {
            System.err.println("IOException on creating channels" + e.getMessage());
        }
    }
}
