package info.kgeorgiy.ja.samodelov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HelloUDPServer implements HelloServer {

    protected ExecutorService threadPool;
    protected ExecutorService serverThread;
    private DatagramSocket datagramSocket;

    private static DatagramPacket receive(DatagramSocket socket) throws IOException {
        DatagramPacket inPacket = Utils.createPacket(socket);
        socket.receive(inPacket);
        return inPacket;
    }


    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Incorrect command line arguments format");
            return;
        }
        int port;
        int threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error in parsing arguments");
            return;
        }

        try (HelloUDPServer helloUDPServer = new HelloUDPServer()) {
            helloUDPServer.start(port, threads);
        }
    }

    @Override
    public void start(int port, int threads) {
        try {
            datagramSocket = new DatagramSocket(port);
            serverThread = Executors.newSingleThreadExecutor();
            threadPool = Executors.newFixedThreadPool(threads);
            serverThread.submit(() -> {
                try {
                    while (!Thread.interrupted()) {
                        DatagramPacket datagramPacket = receive(datagramSocket);
                        threadPool.submit(() -> {
                            String request = Utils.response(Utils.getString(datagramPacket));
                            try {
                                Utils.send(datagramSocket, request, datagramPacket.getSocketAddress());
                            } catch (IOException e) {
                                System.err.println("Sending was failed with IOException");
                            }
                        });
                    }
                } catch (IOException e) {
                    System.err.println("Receiving was failed with IOException");
                }
            });

        } catch (SocketException e) {
            System.err.println("Creating socket was failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        datagramSocket.close();
        threadPool.close();
        serverThread.close();
    }
}
