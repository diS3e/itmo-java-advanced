package info.kgeorgiy.ja.samodelov.i18n;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiConsumer;

public class StatisticsWriter {

    private static String lineFormat(ResourceBundle resourceBundle, String categoryName, String value) {
        return MessageFormat.format(resourceBundle.getString("Line_format"), "\t", categoryName, ": ", value, "\n");

    }

    private static void writeHeader(ResourceBundle resourceBundle,
                                    BufferedWriter writer,
                                    String fileName,
                                    Map<StatisticType, StatisticData<?>> statistic) throws IOException {
        writer.write(MessageFormat.format(resourceBundle.getString("Line_format"), resourceBundle.getString("Title"), "\n", "", ""));
        writer.write(MessageFormat.format(resourceBundle.getString("Line_format"), resourceBundle.getString("Analyse_file"), ": ", fileName, "\n\n"));
        writer.write(MessageFormat.format(resourceBundle.getString("Line_format"), "\t", resourceBundle.getString("General_statistic"),"\n", ""));
        for (var type : StatisticType.values()) {
            writer.write(lineFormat(resourceBundle,
                    resourceBundle.getString("Count_" + type.toString().toLowerCase()),
                    String.valueOf(statistic.get(type).getNumberOfAll())
            ));
        }
        writer.write("\n");
    }

    public static void generateReport(Locale inputLocale,
                                      ResourceBundle resourceBundle,
                                      BufferedWriter writer,
                                      String fileName,
                                      Map<StatisticType, StatisticData<?>> statistic) throws IOException {

        writeHeader(resourceBundle, writer, fileName, statistic);
        for (var type : StatisticType.values()) {
            StatisticData<?> data = statistic.get(type);
            int count = data.getNumberOfAll();
            int uniqWords = data.getNumberOfUnique();
            String min;
            String max;
            Integer minLength;
            Integer maxLength;
            String average;

            BiConsumer<String, String> printer = (String statisticType, String statisticInformation) -> {
                try {
                    writer.write(lineFormat(resourceBundle, resourceBundle.getString(statisticType + type.toString().toLowerCase()), statisticInformation));
                } catch (IOException e) {
                    System.err.println("IOException occurs");
                }
            };

            printer.accept("Statistic_on_", "");
            printer.accept("Count_", Integer.toString(count));
            if (count != 0) {
                switch (type) {
                    case SENTENCES, WORDS -> {
                        min = data.getMinValue(it -> (String) it, Collator.getInstance(inputLocale)::compare);
                        max = data.getMaxValue(it -> (String) it, Collator.getInstance(inputLocale)::compare);
                        minLength = data.getMinValue(it -> ((String) it).length(), Integer::compare);
                        maxLength = data.getMaxValue(it -> ((String) it).length(), Integer::compare);
                        average = String.valueOf(Double.valueOf(data.getReduced(
                                entry -> ((String) entry.getKey()).length() * entry.getValue(),
                                Integer::sum, 0)
                        ) / count);
                    }
                    case MONEY, NUMBERS -> {
                        min = data.getMinValue(it -> new BigDecimal(it.toString()), BigDecimal::compareTo).toString();
                        max = data.getMaxValue(it -> new BigDecimal(it.toString()), BigDecimal::compareTo).toString();
                        minLength = data.getMinValue(it -> it.toString().length(), Integer::compare);
                        maxLength = data.getMaxValue(it -> it.toString().length(), Integer::compare);
                        average = data.getReduced(entry -> new BigDecimal(entry.getKey().toString())
                                                .multiply(new BigDecimal(entry.getValue())),
                                        BigDecimal::add, new BigDecimal(0))
                                .divide(new BigDecimal(count), RoundingMode.CEILING).toString();
                    }
                    case DATES -> {
                        min = data.getMinValue(it -> (Date) it, Date::compareTo).toString();
                        max = data.getMaxValue(it -> (Date) it, Date::compareTo).toString();
                        minLength = data.getMinValue(it -> it.toString().length(), Integer::compare);
                        maxLength = data.getMaxValue(it -> it.toString().length(), Integer::compare);
                        average = new Date(data.getReduced(entry -> new BigDecimal(((Date) entry.getKey()).getTime())
                                                .multiply(new BigDecimal(entry.getValue())),
                                        BigDecimal::add, new BigDecimal(0))
                                .divide(new BigDecimal(count), RoundingMode.CEILING).longValue()).toString();
                    }
                    default -> throw new RuntimeException("Unexpected enum type");
                }


                printer.accept("Uniq_count_", Integer.toString(uniqWords));
                printer.accept("Min_", min);
                printer.accept("Max_", max);
                printer.accept("Min_length_", minLength.toString());
                printer.accept("Max_length_", maxLength.toString());
                printer.accept("Average_", average);
            }
            writer.write("\n");
        }
    }
}
