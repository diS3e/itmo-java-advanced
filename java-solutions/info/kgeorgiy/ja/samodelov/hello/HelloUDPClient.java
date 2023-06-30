package info.kgeorgiy.ja.samodelov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPClient implements HelloClient {

    public static void main(String[] args) {
        // :NOTE: похоже на Server, можно вынести в Utils
        if (args == null || args.length != 5) {
            System.err.println("Incorrect command line arguments format");
            return;
        }
        int port;
        int threads;
        int requests;
        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Error in parsing arguments");
            return;
        }

        HelloUDPClient helloUDPClient = new HelloUDPClient();
        helloUDPClient.run(args[0], port, args[2], threads, requests);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try (ExecutorService threadPool = Executors.newFixedThreadPool(threads)) {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
            // :NOTE: можно IntStream, чтобы без final int
            for (int i = 1; i <= threads; i++) {
                final int finalI = i;
                threadPool.submit(() -> {
                    try (DatagramSocket datagramSocket = new DatagramSocket()) {
                        datagramSocket.setSoTimeout(Utils.TIMEOUT);
                        for (int j = 1; j <= requests; j++) {
                            String message = Utils.createMessage(prefix, finalI, j);
                            while (true) {
                                try {
                                    String responseMessage = Utils.request(message, datagramSocket, inetSocketAddress);
                                    if (Utils.isCorrectResponse(responseMessage, finalI, j)) {
                                        System.out.println("Requested: " + message);
                                        break;
                                    }
                                } catch (IOException e) {
                                    System.out.println("IOException was handled" + message);
                                }
                            }
                        }
                    } catch (SocketException e) {
                        System.err.println("Unable to create socket");
                    }
                });
            }
        }
    }
}

