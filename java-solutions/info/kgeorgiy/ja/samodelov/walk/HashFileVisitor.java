package info.kgeorgiy.ja.samodelov.walk;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashFileVisitor implements FileVisitor<Path> {

    private final BufferedWriter writer;
    private final byte[] buffer = new byte[4096];
    public final String exceptionHash;
    private final MessageDigest md = MessageDigest.getInstance("SHA-256");

    public HashFileVisitor(BufferedWriter writer) throws NoSuchAlgorithmException {
        this.writer = writer;
        exceptionHash = "0".repeat(md.getDigestLength() << 1);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    public void getHash(Path file) throws IOException {
        String message = "";
        try (InputStream inputStream = Files.newInputStream(file)) {
            int size;
            while ((size = inputStream.read(buffer)) >= 0) {
                md.update(buffer, 0, size);
            }
            byte[] hash = md.digest();
            message = String.format("%0" + (hash.length << 1) + "x", new BigInteger(1, hash));
        } catch (IOException e) {
            message = exceptionHash;
        } finally {
            write(message, file.toString());
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        getHash(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        write(exceptionHash, file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    public void write(String hash, String file) throws IOException {
        writer.write(String.format("%s %s" + System.lineSeparator(), hash, file));
    }
}
