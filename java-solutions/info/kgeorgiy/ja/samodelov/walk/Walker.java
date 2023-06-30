package info.kgeorgiy.ja.samodelov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walker {
    public static final String DEFAULT = "DEFAULT";
    public static final String RECURSIVE = "RECURSIVE";
    protected static void walk(String inputString, String outputString, String walkerType) throws Exception {
        Path inputPath;
        Path outputPath;
        try {
            inputPath = Paths.get(inputString);
            outputPath = Paths.get(outputString);
            if (outputPath.getParent() != null && Files.notExists(outputPath.getParent())) {
                Files.createDirectories(outputPath.getParent());
            }
        } catch(InvalidPathException e) {
            System.err.println("Invalid path string of input or output");
            return;
        }

        try (BufferedReader input = Files.newBufferedReader(inputPath)) {
            try (BufferedWriter output = Files.newBufferedWriter(outputPath)) {
                HashFileVisitor visitor = new HashFileVisitor(output);
                String pathString;
                while ((pathString = input.readLine()) != null) {
                    try {
                        Path path = Paths.get(pathString);
                        switch (walkerType){
                            case DEFAULT -> visitor.getHash(path);
                            case RECURSIVE -> Files.walkFileTree(path, visitor);
                            default -> System.err.println("Unexpected walk type");
                        }
                    } catch (InvalidPathException | IOException e) {
                        visitor.write(visitor.exceptionHash, pathString);
                    }
                }
            } catch (IOException e) {
                System.err.println("Output file wasn't open");
            }
        } catch (IOException e) {
            System.err.println("Input file wasn't open");
        }

    }

    public static void invoke(String[] args, String walkerType) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Incorrect format of arguments. Expected \"file.in file.out\"");
            return;
        }
        try {
            walk(args[0], args[1], walkerType);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
