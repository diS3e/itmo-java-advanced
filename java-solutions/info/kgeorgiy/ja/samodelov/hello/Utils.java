package info.kgeorgiy.ja.samodelov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final int TIMEOUT = 239;

    public static final List<NumberFormat> FORMATS = Arrays.stream(Locale.getAvailableLocales()).map(NumberFormat::getNumberInstance).toList();

    private static String normalizeNumbersInString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                sb.append(Character.getNumericValue(s.charAt(i)));
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    public static boolean isCorrectResponse(String response, int thread, int request) {
        Pattern pat = Pattern.compile("[0-9]+");
        Matcher matcher = pat.matcher(normalizeNumbersInString(response));
        List<String> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers.size() == 2 &&
                FORMATS.stream().anyMatch(format ->
                        format.format(thread).equals(numbers.get(0)) &&
                                format.format(request).equals(numbers.get(1))
                );

    }


    public static String getString(DatagramPacket packet) {
        return getString(packet.getData(), packet.getOffset(), packet.getLength());
    }

    public static String getString(byte[] data, int offset, int length) {
        return new String(data, offset, length, CHARSET);
    }

    public static void setString(DatagramPacket packet, String string) {
        byte[] bytes = getBytes(string);
        packet.setData(bytes);
        packet.setLength(packet.getData().length);
    }

    public static byte[] getBytes(String string) {
        return string.getBytes(CHARSET);
    }

    public static DatagramPacket createPacket(DatagramSocket socket) throws SocketException {
        return new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
    }

    public static String request(String string, DatagramSocket socket, SocketAddress address) throws IOException {
        send(socket, string, address);
        return receive(socket);
    }

    public static String receive(DatagramSocket socket) throws IOException {
        DatagramPacket inPacket = createPacket(socket);
        socket.receive(inPacket);
        return getString(inPacket);
    }

    public static void send(DatagramSocket socket, String request, SocketAddress address) throws IOException {
        DatagramPacket outPacket = new DatagramPacket(new byte[0], 0);
        setString(outPacket, request);
        outPacket.setSocketAddress(address);
        socket.send(outPacket);
    }

    public static String createMessage(String prefix, int numberThread, int numberRequest) {
        return String.join("", prefix, Integer.toString(numberThread), "_", Integer.toString(numberRequest));
    }

    public static String response(String request) {
        return String.format("Hello, %s", request);
    }

}
