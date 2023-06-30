package info.kgeorgiy.ja.samodelov.i18n;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.*;
import java.util.*;
import java.util.function.*;

public class TextStatistics {
    private static class Borders {
        Integer begin;
        Integer end;

        private Borders(Integer first, Integer second) {
            this.begin = first;
            this.end = second;
        }

        public Integer begin() {
            return begin;
        }

        public Integer end() {
            return end;
        }

    }

    private static class TextIterator{
        Borders borders;
        Consumer<Borders> next;

        private TextIterator(BreakIterator breakIterator) {
            this.borders = new Borders(breakIterator.first(), breakIterator.next());
            this.next = border -> {
                border.begin = border.end;
                border.end = breakIterator.next();
            };
        }
        public Borders border() {
            return borders;
        }

        public Consumer<Borders> next() {
            return next;
        }
    }

    private static class ParsingResult<T> {
        T value;
        Integer indexOfEnd;

        private ParsingResult(T value, Integer index) {
            this.value = value;
            this.indexOfEnd = index;
        }

    }
    private static final List<Integer> dateFormatList = List.of(DateFormat.SHORT, DateFormat.MEDIUM,
            DateFormat.LONG, DateFormat.FULL);
    private static <T, R> StatisticData<R> getAbstractStatistic(String text,
                                                                BreakIterator breakIterator,
                                                                BiPredicate<ParsingResult<T>, ArrayList<T>> predicate,
                                                                Function<T, R> mapper,
                                                                Function<Borders, ParsingResult<T>> parser,
                                                                boolean rememberTokenEnd) {
        breakIterator.setText(text);
        ArrayList<T> instances = new ArrayList<>();
        var textIterable = new TextIterator(breakIterator);
        int tokenEnd = 0;
        for (var iterator = textIterable.border(); iterator.end() != BreakIterator.DONE; textIterable.next().accept(iterator)) {
            if (iterator.begin() < tokenEnd) {
                continue;
            }
            ParsingResult<T> instance = parser.apply(iterator);

            if (predicate.test(instance, instances)) {
                instances.add(instance.value);
                if (rememberTokenEnd) {
                    tokenEnd = instance.indexOfEnd;
                }
            }
        }
        return new StatisticData<>(instances.stream().map(mapper).toList());
    }

    private static <R> StatisticData<R> getParsableStatistic(String text, Locale locale,
                                                             BiFunction<String, ParsePosition, R> parser,
                                                             boolean rememberTokenEnd) {
        return getAbstractStatistic(text, BreakIterator.getWordInstance(locale),
                (instance, instances) -> instance != null && instance.value != null, Function.identity(),
                pair -> {
                    ParsePosition parsePosition = new ParsePosition(pair.begin);
                    R result = parser.apply(text, parsePosition);
                    return new ParsingResult<>(result, parsePosition.getIndex());
                }, rememberTokenEnd);
    }

    public static Map<StatisticType, StatisticData<?>> getStatistic(String text, Locale locale) {
        Map<StatisticType, StatisticData<?>> statistic = new HashMap<>();

        statistic.put(StatisticType.SENTENCES, getAbstractStatistic(
                text,
                BreakIterator.getSentenceInstance(locale),
                (instance, instances) -> true,
                Function.identity(),
                pair -> new ParsingResult<>(text.substring(pair.begin, pair.end).trim(), pair.end),
                false));

        statistic.put(StatisticType.WORDS, getAbstractStatistic(text, BreakIterator.getWordInstance(locale),
                (instance, instances) ->
                        instance != null && !((String )instance.value).isEmpty() && Character.isLetter(((String)instance.value).charAt(0)),
                it -> ((String) it).toLowerCase(locale),
                pair -> new ParsingResult<>(text.substring(pair.begin, pair.end).trim(), pair.end), false));

        statistic.put(StatisticType.NUMBERS,
                getParsableStatistic(text, locale, NumberFormat.getNumberInstance(locale)::parse, false));

        statistic.put(StatisticType.MONEY,
                getParsableStatistic(text, locale, NumberFormat.getCurrencyInstance(locale)::parse, false));

        statistic.put(StatisticType.DATES, getParsableStatistic(text, locale,
                (txt, pos) -> dateFormatList.stream()
                        .map(format -> DateFormat.getDateInstance(format, locale).parse(txt, pos))
                        .filter(Objects::nonNull).findAny().orElse(null), true));
        return statistic;
    }
    public static void main(String[] args) {
        if (args == null || args.length != 4 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("You must use this format: inputLocale outputLocale pathInputFile pathOutputFile");
            return;
        }
        Locale outputLocale;
        Locale locale;
        try {
            locale = new Locale.Builder().setLanguageTag(args[0]).build();
            outputLocale = new Locale.Builder().setLanguageTag(args[1]).build();
        } catch (IllformedLocaleException e) {
            System.err.println("Unexpected locale: " + e.getMessage());
            return;
        }
        ResourceBundle outputFileBundle;
        try {
            outputFileBundle = ResourceBundle.getBundle("info.kgeorgiy.ja.samodelov.i18n.Bundle",
                    Locale.forLanguageTag(args[1]));
        } catch (MissingResourceException e) {
            System.err.println("Can't find bundle: " + e.getMessage());
            return;
        }

        Path inputPath;
        Path outputPath;
        try {
            inputPath = Path.of(args[2]);
            outputPath = Path.of(args[3]);
        } catch (InvalidPathException e) {
            System.err.println(outputFileBundle.getString("Invalid_path"));
            return;
        }

        if (!outputLocale.getLanguage().equals("ru") && !outputLocale.getLanguage().equals("en")) {
            System.err.println(outputFileBundle.getString("Output_locale"));
            return;
        }

        String text;
        try {
            text = Files.readString(inputPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("I/O error occurs reading from the file: " + e.getMessage());
            return;
        }

        var statistic = getStatistic(text, locale);


        BufferedWriter outputWriter;
        try {
            outputWriter = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Can't open output file: " + e.getMessage());
            return;
        }

        try {
            StatisticsWriter.generateReport(locale, outputFileBundle, outputWriter, args[2], statistic);
        } catch (IOException e) {
            System.err.println("IOException in writing in file: " + e.getMessage());
        }


        try {
            outputWriter.close();
        } catch (IOException e) {
            System.err.println("Can't close output file:" + e.getMessage());
        }
    }
}