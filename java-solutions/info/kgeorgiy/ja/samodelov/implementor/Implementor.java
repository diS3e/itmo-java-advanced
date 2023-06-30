package info.kgeorgiy.ja.samodelov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Implementor class
 * Realization of interface {@link JarImpler}
 */
public class Implementor implements JarImpler {

    /**
     * Default constructor
     */
    public Implementor() {

    }

    /**
     * Comparator to check generic implementations
     */
    private static final Comparator<Generator.ImplementedMethods> COMPARATOR = Comparator.comparing(
        (Generator.ImplementedMethods a) -> a.method.getReturnType().equals(Object.class));

//    class A {
//
//    }
//    class B extends A {
//
//    }
//
//    abstract class C {
//        abstract A method();
//    }
//
//    abstract class D extends C{
//        abstract B method();
//    }
//
//    class DImpl extends D {
//        // какой тип здесь?
//    }

    /**
     * Return true if {@code token} is valid to implement
     * <p>
     * Token isn't valid, if it's
     * <ul>
     *     <li>Primitive type</li>
     *     <li>Array type</li>
     *     <li>Enum type</li>
     *     <li>Has private modifier</li>
     *     <li>Has final modifier</li>
     * </ul>
     *
     * @param token type token to create implementation for.
     * @return {@code true} if token valid else return {@code false}
     */
    private boolean isValidToken(Class<?> token) {
        int modifiers = token.getModifiers();

        return !token.isPrimitive()
                && !token.isArray()
                && !token.isAssignableFrom(Enum.class)
                && !Modifier.isPrivate(modifiers)
                && !Modifier.isFinal(modifiers);
    }

    /**
     * Generate code of realizable class and write to the file
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if {@code isValidToken(token) returns false or IO exception was handled}
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!isValidToken(token)) throw new ImplerException("Incorrect type token");

        try (BufferedWriter writer = Files.newBufferedWriter(getPathToClass(token, root))) {
            String string  = Generator.generate(token);
            writer.write(string);
        } catch (IOException e) {
            throw new ImplerException("Can't create implemented class");
        }
    }

    /**
     * Create parent directory and return {@link Path} to output file
     * Using {@code File.separatorChar} as separator in path
     *
     * @param token type token to create implementation for.
     * @param root  root directory
     * @return {@link Path} to output file
     * @throws IOException if create directory was failed
     */
    private Path getPathToClass(final Class<?> token, final Path root) throws IOException {
        Path parentsPath = root.resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(String.format("%s.java", Generator.getNameWithSuffix(token)));

        if (parentsPath.getParent() != null) {
            Files.createDirectories(parentsPath.getParent());
        }
        return parentsPath;
    }

    /**
     * Check if given arguments is valid.
     * <p>
     * Expected two or three arguments. If got three arguments, then check if third is "-jar"
     * Default usage of this function is check command line arguments.
     *
     * @param args arguments
     * @return {@code true} if arguments is valid else return {@code false}
     */
    private static boolean isValidArgs(String[] args) {
        // :NOTE: 2 аргумента это корректно
        if (args == null || !(args.length == 3 && args[0].equals("-jar") || args.length == 2)) {
            System.err.println("Incorrect arguments");
            return false;
        }
        return true;
    }

    /**
     * Run implementor with command line arguments
     * Has two type of work:
     * <ul>
     *     <li>Expected two arguments: classname and path to output file.
     *     Write realization of class to output file</li>
     *     <li>Expected three arguments: "-jar", classname and path to .jar file
     *     Write realization of class,  convert it to .jar file and put it on path to .jar file </li>
     * </ul>
     *
     * @param args command line arguments
     * @throws ImplerException        if catch IO exception or compilation in .jar mode was failed
     * @throws ClassNotFoundException if default {@link ClassLoader} can't load class with arguments name
     */
    public static void main(String[] args) throws ImplerException, ClassNotFoundException {
        if (!isValidArgs(args)) {
            return;
        }

        Implementor implement = new Implementor();
        Class<?> token;
        Path path;
        if (args.length == 3) {
            token = Class.forName(args[1]);
            path = Path.of(args[2]);
            implement.implementJar(token, path);
        } else {
            token = Class.forName(args[0]);
            path = Path.of(args[1]);
            implement.implement(token, path);
        }
    }

    /**
     * Generate <var>.jar</var> file, contains compiled realization of {@code token}
     * <p>
     * Based on {@link #implement(Class, Path)}
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if IO exception was thrown or compilation was failed
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (!isValidToken(token)) throw new ImplerException("Incorrect type token");
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Unable to create temp directory", e);
        }
        try {
            implement(token, tempDir);
            compileFile(token, tempDir,
                    tempDir.resolve((token.getPackageName() + "." + Generator.getNameWithSuffix(token))
                            .replace(".", File.separator) + ".java").toAbsolutePath().toString());
            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Eugene Samodelov");
            try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                final String className = token.getPackageName().replace('.', '/')
                        + "/"
                        + Generator.getNameWithSuffix(token)
                        + ".class";
                writer.putNextEntry(new ZipEntry(className));
                Files.copy(Paths.get(tempDir.toString(), className), writer);
            } catch (IOException e) {
                throw new ImplerException("IOException in writing in .jar");
            }
        } finally {
            try {
                Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                System.out.println("Can't delete temporary directory");
            }
        }
    }

    /**
     * Compile file
     * <p>
     * Based on Georgiy Korneev code.
     *
     * @param token type token to create implementation for.
     * @param root  root directory
     * @param file  to compile file
     * @throws ImplerException if compilation was failed or can't load classpath
     */
    public static void compileFile(final Class<?> token, final Path root, final String file) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compiler not provided");
        }
        final String classpath;
        try {
            classpath = root + File.pathSeparator +
                    Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new ImplerException("Can't load URI");
        }
        final String[] args = Stream.concat(Stream.of(file), Stream.of("-cp", classpath,
                "-encoding", StandardCharsets.UTF_8.name())).toArray(String[]::new);

        final int exitCode = compiler.run(null, null, System.out, args);
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code " + exitCode);
        }
    }

    /**
     * Subclass, which generate code
     */
    private abstract static class Generator {

        /**
         * Unused constructor
         */
        private Generator(){

        }
        /**
         * Suffix of class name.
         * Default value is {@code "Impl"}
         */
        public static final String CLASS_SUFFIX = "Impl";
        /**
         * Line separator. Using system line separator
         */
        private static final String LINE_SEPARATOR = System.lineSeparator();
        /**
         * Words separator. Default value is whitespace
         */
        private static final String WHITESPACE = " ";
        /**
         * Function arguments separator
         */
        private static final String PARAMETER_DELIMITER = "," + WHITESPACE;
        /**
         * Empty string
         */
        private static final String EMPTY = "";

        /**
         * Return name of realizing class
         *
         * @param token type token to create implementation for.
         * @return name of realizing class
         */
        private static String getNameWithSuffix(Class<?> token) {
            return String.format("%s%s", token.getSimpleName(), CLASS_SUFFIX);
        }

        /**
         * Return string with {@link #LINE_SEPARATOR}
         *
         * @param format format string as in {@link String#format(String, Object...)}
         * @param args   arguments for format string as in {@link String#format(String, Object...)}
         * @return formatted string with {@link #LINE_SEPARATOR}
         */
        private static String writeStringWithEndLine(String format, Object... args) {
            return String.format(format + LINE_SEPARATOR, args);
        }

        /**
         * Return string of package of realizing class
         *
         * @param token type token to create implementation for.
         * @return package of realizng class or {{@link #EMPTY}} if package not exist
         */
        private static String generatePackage(final Class<?> token) {
            // :NOTE: token.getPackageName() == ""
            return (token.getPackage() == null) ? EMPTY : writeStringWithEndLine("package %s;", token.getPackage().getName());
        }

        /**
         * Return string of definition of realizing class
         *
         * @param token type token to create implementation for.
         * @return definition of realizing class
         */
        private static String generateClassHead(final Class<?> token) {
            return writeStringWithEndLine("public class %s %s %s {", getNameWithSuffix(token), (token.isInterface()) ? "implements" : "extends", token.getCanonicalName());
        }

        /**
         * Return string of arguments of method
         *
         * @param method                  is method or constructor
         * @param enableTypesVisibilities if {@code true} then add types to arguments else not
         * @return string of arguments of method
         */
        private static String generateArguments(Executable method, boolean enableTypesVisibilities) {

            return String.format("(%s)", Arrays.stream(method.getParameters())
                    .map(
                            parameter -> (enableTypesVisibilities ? parameter.getType().getCanonicalName() : EMPTY) +
                                    WHITESPACE + parameter.getName())
                    .collect(Collectors.joining(PARAMETER_DELIMITER)));
        }

        /**
         * Return string of checked exception, which method can throw
         *
         * @param executable method or constructor
         * @return string of checked exception, which method can throw
         */
        private static String generateExceptions(Executable executable) {
            Class<?>[] exceptions = executable.getExceptionTypes();
            return exceptions.length == 0 ? EMPTY : writeStringWithEndLine("throws %s",
                    Arrays.stream(exceptions).map(Class::getCanonicalName).collect(Collectors.joining(PARAMETER_DELIMITER)));
        }

        /**
         * Put {@code methods} to {@code allMethods} if it's not contains
         * @param allMethods collections to put in
         * @param methods methods to put
         */
        private static void putMethods(HashSet<ImplementedMethods> allMethods, Stream<ImplementedMethods> methods) {
            allMethods.addAll(methods.sorted(COMPARATOR).toList());
        }

        /**
         * Return all methods, which realized in parents of class
         * <p>
         * If method have realization in children, then get it and skip abstract method
         * Methods comparing by hash of name, arguments and return type
         *
         * @param token type token to create implementation for
         * @return {@link HashSet} containing all methods
         */


        private static HashSet<ImplementedMethods> getAllMethodsFromParents(Class<?> token, int depth) throws ImplerException {
            if (token == null) {
                return new HashSet<>();
            }
            HashSet<ImplementedMethods> allMethods = new HashSet<>();
            putMethods(allMethods, Arrays.stream(token.getMethods()).map(ImplementedMethods::new));
            putMethods(allMethods, Arrays.stream(token.getDeclaredMethods()).map(ImplementedMethods::new));
            if (depth == 0) {
                for (var method: allMethods) {
                    if (Modifier.isPrivate(method.method.getReturnType().getModifiers())) {
                        throw new ImplerException("Can't parse when return type has private modifier");
                    }

                    for(var type: method.method.getParameterTypes()) {
                        if (Modifier.isPrivate(type.getModifiers())) {
                            throw new ImplerException("Can't parse when args has private modifier");
                        }
                    }
                }
            }


            putMethods(allMethods, getAllMethodsFromParents(token.getSuperclass(), depth + 1).stream());
            return allMethods;
        }

        /**
         * Return string of method
         *
         * @param returnType string of return type of method. Empty for constructors
         * @param name       string of method name
         * @param arguments  string of methods arguments
         * @param exceptions string of exceptions
         * @param body       string of method body
         * @return string of method
         */
        private static String getStringOfExecutable(String returnType, String name, String arguments, String exceptions, String body) {
            return String.format("\tpublic %s %s%s %s {%s\t\t%s;%s\t}%s",
                    returnType, name,
                    arguments, exceptions, LINE_SEPARATOR,
                    body, LINE_SEPARATOR, LINE_SEPARATOR);
        }

        /**
         * Return string of default realization of all abstract methods
         * Skip abstract methods, which have realization in parents
         *
         * @param token type token to create implementation for.
         * @return string of default realization of all abstract methods.
         */
        private static String generateMethods(Class<?> token) throws ImplerException {
            HashSet<ImplementedMethods> allMethods = getAllMethodsFromParents(token, 0);
            return allMethods.stream()
                    .map(ImplementedMethods::method)
                    .filter(method -> Modifier.isAbstract(method.getModifiers()))
                    .map(method -> getStringOfExecutable(method.getReturnType().getCanonicalName(),
                            method.getName(),
                            generateArguments(method, true),
                            generateExceptions(method),
                            "return " + getDefaultOfType(method.getReturnType()))
                    ).collect(Collectors.joining(LINE_SEPARATOR));
        }

        /**
         * Return string of all constructors
         *
         * @param token type token to create implementation for.
         * @return stirng of all constructors
         * @throws ImplerException when realizable class is utilities
         */
        private static String generateConstructors(final Class<?> token) throws ImplerException {
            List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                    .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers())).toList();
            if (constructors.stream().anyMatch(method -> Arrays.stream(method.getParameterTypes()).anyMatch(type -> Modifier.isPrivate(type.getModifiers())))) {
                throw new ImplerException("Can't parse when constructor has private modifier");
            }

            if (constructors.size() == 0) {
                throw new ImplerException("Can't extends of utilities classes");
            }
            return constructors.stream().map(constructor ->
                    getStringOfExecutable(EMPTY, getNameWithSuffix(token),
                            generateArguments(constructor, true),
                            generateExceptions(constructor),
                            "super" + generateArguments(constructor, false))
            ).collect(Collectors.joining(LINE_SEPARATOR));
        }

        /**
         * Return string of class code
         *
         * @param token type token to create implementation for.
         * @return string of class code
         * @throws ImplerException if utilities class was given
         */
        public static String generate(final Class<?> token) throws ImplerException {
            return String.join(LINE_SEPARATOR,
                    generatePackage(token),
                    generateClassHead(token),
                    (Modifier.isInterface(token.getModifiers())) ? EMPTY : generateConstructors(token),
                    generateMethods(token),
                    "}", LINE_SEPARATOR);
        }

        /**
         * String of default value
         * <ul>
         *     <li>boolean -> "true"</li>
         *     <li>void -> {@link #EMPTY}</li>
         *     <li>other primary -> "0"</li>
         *     <li>non primary -> "null"</li>
         * </ul>
         *
         * @param token type token to create implementation for.
         * @return string of default value
         */
        private static String getDefaultOfType(Class<?> token) {
            if (token.isPrimitive()) {
                if (token == boolean.class) {
                    return "true";
                } else if (token == void.class) {
                    return EMPTY;
                } else {
                    return "0";
                }
            } else {
                return "null";
            }
        }

        /**
         * Method wrapper
         * Can check methods on equals
         *
         * @param method method, which need to wrap
         */
        private record ImplementedMethods(Method method) {
            /**
             * Checks method for equality
             * Method using hash of {@link Method#getReturnType()}, {@link Method#getName()} and {@link Method#getParameterTypes()}
             *
             * @param o the reference object with which to compare.
             * @return {@code true} if object is equal to this method else {@code false}
             */
            @Override
            public boolean equals(final Object o) {
                if (getClass() != o.getClass()) {
                    return false;
                }

                ImplementedMethods casted = (ImplementedMethods) o;

                return method.getName().equals(casted.method.getName())
                        && Arrays.equals(method.getParameterTypes(), casted.method.getParameterTypes());
            }

            /**
             * Return hash code of method
             * * Method using hash of {@link Method#getReturnType()}, {@link Method#getName()} and {@link Method#getParameterTypes()}
             *
             * @return hash code of method
             */
            @Override
            public int hashCode() {
                return method.getName().hashCode()
                        + Arrays.hashCode(method.getParameterTypes());
            }
        }
    }
}