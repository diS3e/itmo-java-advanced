package info.kgeorgiy.ja.samodelov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IterativeParallelism implements AdvancedIP {

    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }


    private <T, P, U> U parallelRunning(int threadsCount,
                                        List<? extends T> values,
                                        Function<List<T>, P> function,
                                        Function<List<P>, U> collector) throws InterruptedException {

        List<List<? extends T>> splitValues = IntStream.range(0, Math.min(threadsCount, values.size())).mapToObj(index -> {
            if (threadsCount < values.size()) {
                int blockSize = values.size() / threadsCount;
                int tailSize = values.size() % threadsCount;
                int from = blockSize * index + Math.min(index, tailSize);
                return values.subList(from, from + blockSize + ((index < tailSize) ? 1 : 0));
            } else {
                return values.subList(index, index + 1);
            }
        }).toList();

        List<P> results;
        if (parallelMapper == null) {
            results = new ArrayList<>(Collections.nCopies(splitValues.size(), null));
            List<Thread> threadList = IntStream.range(0, splitValues.size()).mapToObj(index -> new Thread(
                    () -> results.set(index, function.apply(splitValues.get(index).stream().map(it -> (T) it).toList()))
            )).toList();
            threadList.forEach(Thread::start);
            boolean isInterrupted = false;
            InterruptedException exception = new InterruptedException("");
            for (int i = 0; i < threadList.size(); i++) {
                try {
                    threadList.get(i).join();
                } catch (InterruptedException e) {
                    exception.addSuppressed(e);
                    if (!isInterrupted) {
                        for (int j = i; j < threadList.size(); j++) {
                            threadList.get(j).interrupt();
                        }
                        isInterrupted = true;
                    }
                    // :NOTE: join
                }
            }
            if (isInterrupted) {
                throw exception;
            }
        } else {
            results = parallelMapper.map(value -> function.apply(value.stream().map(it -> (T) it).toList()), splitValues);
        }
        return collector.apply(results);
    }


    @Override
    public <T> T maximum(int threads, List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        return parallelRunning(threads, values,
                stream -> stream.stream().max(comparator).orElse(null),
                stream -> stream.stream().max(comparator).orElse(null));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        return parallelRunning(threads, values,
                stream -> stream.stream().min(comparator).orElse(null),
                stream -> stream.stream().min(comparator).orElse(null));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return parallelRunning(threads, values,
                stream -> stream.stream().allMatch(predicate),
                booleanStream -> booleanStream.stream().allMatch(it -> it));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public <T> int count(int threads, List<? extends T> values,
                         Predicate<? super T> predicate) throws InterruptedException {
        return parallelRunning(threads, values,
                stream -> stream.stream().filter(predicate).count(),
                integerStream -> integerStream.stream().mapToInt(Long::intValue).sum());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelRunning(threads, values,
                stream -> stream.stream().map(Object::toString).collect(Collectors.joining("")),
                stream -> String.join("", stream));
    }

    private <T, U> List<U> listConverter(int threads,
                                         List<? extends T> values,
                                         Function<List<? extends T>,
                                                 List<? extends U>> converter) throws InterruptedException {
        return parallelRunning(threads, values,
                converter::apply,
                streamStream -> streamStream.stream()
                        .flatMap(stream -> stream.stream().map(it -> (U) it)).toList());
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values,
                              Predicate<? super T> predicate) throws InterruptedException {
        return listConverter(threads, values, stream -> stream.stream().filter(predicate).toList());
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values,
                              Function<? super T, ? extends U> f) throws InterruptedException {
        return listConverter(threads, values, stream -> stream.stream().map(f).toList());
    }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return parallelRunning(threads, values,
                stream -> stream.stream().reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.stream().reduce(monoid.getIdentity(), monoid.getOperator()));
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return parallelRunning(threads, values,
                stream -> stream.stream().map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.stream().reduce(monoid.getIdentity(), monoid.getOperator()));
    }
}
