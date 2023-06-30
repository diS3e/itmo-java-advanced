package info.kgeorgiy.ja.samodelov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threadList;
    private final Deque<Runnable> taskDeque = new ArrayDeque<>();
    private final Runnable MAPPER_THREAD = () -> {
        try {
            while (!Thread.interrupted()) {
                Runnable runnable;
                synchronized (taskDeque) {
                    while (taskDeque.isEmpty()) {
                        taskDeque.wait();
                    }
                    runnable = taskDeque.poll();
                }
                runnable.run();
            }
        } catch (InterruptedException e) {
            System.err.print("");
        } finally {
            Thread.currentThread().interrupt();
        }
    };

    public ParallelMapperImpl(int threads) {
        threadList = new ArrayList<>(threads);
        IntStream.range(0, threads).forEach(i -> threadList.add(new Thread(MAPPER_THREAD)));
        threadList.forEach(Thread::start);
    }

    private static class ConcurrentList<T> {
        private final List<T> list;
        private int processedObjects = 0;

        private ConcurrentList(int size) {
            this.list = new ArrayList<>(Collections.nCopies(size, null));
        }

        public synchronized void set(int index, T value) {
            list.set(index, value);
            processedObjects++;
            if (processedObjects == list.size()) {
                notify();
            }
        }

        public synchronized List<T> getList() throws InterruptedException {
            while (processedObjects < list.size()) {
                wait();
            }
            return list;
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        ConcurrentList<R> result = new ConcurrentList<>(args.size());
        IntStream.range(0, args.size()).forEach(i -> {
            synchronized (taskDeque) {
                taskDeque.add(() -> result.set(i, f.apply(args.get(i))));
                taskDeque.notifyAll();
            }
        });
        return result.getList();
    }

    @Override
    public void close() {
        threadList.forEach(thread -> {
                    thread.interrupt();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        System.err.print("");
                    }
                }
        );
    }
}
