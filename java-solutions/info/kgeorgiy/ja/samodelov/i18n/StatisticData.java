package info.kgeorgiy.ja.samodelov.i18n;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class StatisticData<T> {
    private final Map<T, Integer> valueToNumber = new HashMap<>();

    public StatisticData(List<T> instances) {
        instances.forEach(it -> valueToNumber.merge(it, 1, Integer::sum));
    }

    public int getNumberOfAll() {
        return valueToNumber.values().stream().mapToInt(it -> it).sum();
    }

    public int getNumberOfUnique() {
        return valueToNumber.size();
    }

    public <R> R getMinValue(Function<T, R> mapper, Comparator<R> comparator) {
        return valueToNumber.keySet().stream().map(mapper).min(comparator).orElse(null);
    }

    public <R> R getMaxValue(Function<T, R> mapper, Comparator<R> comparator) {
        return valueToNumber.keySet().stream().map(mapper).max(comparator).orElse(null);
    }

    public <R> R getReduced(Function<Map.Entry<T, Integer>, R> mapper, BinaryOperator<R> reducer, R startValue) {
        return valueToNumber.entrySet().stream().map(mapper).reduce(startValue, reducer);
    }
}
