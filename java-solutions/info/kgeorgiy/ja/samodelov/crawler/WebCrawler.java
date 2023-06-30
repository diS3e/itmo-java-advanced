package info.kgeorgiy.ja.samodelov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final ExecutorService downloadTreads;
    private final ExecutorService extractorTreads;
    private final int perHost;
    private final ConcurrentHashMap<String, HostWorker> hostMap = new ConcurrentHashMap<>();

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloadTreads = Executors.newFixedThreadPool(downloaders);
        this.extractorTreads = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, null);
    }

    public static void main(String[] args) throws IOException {
        if (args == null || args.length < 1 || args.length > 5) {
            System.err.println("Need to get arguments on this pattern: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        int depth = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int downloaders = args.length > 2 ? Integer.parseInt(args[2]) : 4;
        int extractors = args.length > 3 ? Integer.parseInt(args[3]) : 4;
        int perHost = args.length > 4 ? Integer.parseInt(args[4]) : 4;
        String url = args[0];


        Downloader downloader = new CachingDownloader(10.0);
        try (WebCrawler webCrawler = new WebCrawler(downloader, downloaders, extractors, perHost)) {
            webCrawler.download(url, depth);
        }
    }

    private void downloadLinks(String url, Set<String> result, ConcurrentHashMap<String, IOException> exceptions,
                               Set<String> visitedLinks, Phaser phaser, Set<String> newLayer, int depth, boolean needToAddHosts) {
        // :NOTE: огромный try
        //fixed
        String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            exceptions.put(url, e);
            return;
        }
        if (!needToAddHosts) {
            if (!hostMap.containsKey(host)) {
                return;
            }
        }
        HostWorker hostWorker = hostMap.computeIfAbsent(host, string -> new HostWorker());
        phaser.register();
        hostWorker.addTask(() -> {
            try {
                Document document = downloader.download(url);
                result.add(url);
                visitedLinks.add(url);
                if (depth != 1) {
                    phaser.register();
                    extractorTreads.submit(() -> {
                        try {
                            for (var link : document.extractLinks()) {
                                if (!visitedLinks.contains(link)) {
                                    visitedLinks.add(link);
                                    newLayer.add(link);
                                }
                            }
                        } catch (IOException e) {
                            //
                        }
                        phaser.arriveAndDeregister();
                    });
                }
            } catch (IOException e) {
                exceptions.put(url, e);
            }
            phaser.arriveAndDeregister();
            hostWorker.runTask();
        });
    }

    @Override
    public void close() {
        extractorTreads.close();
        downloadTreads.close();
        // :NOTE: join
        //fixed
    }

    @Override
    public Result download(String url, int depth, List<String> hosts) {
        boolean needToAddHosts = true;
        if (hosts != null) {
            needToAddHosts = false;
            hosts.forEach(host -> hostMap.computeIfAbsent(host, string -> new HostWorker()));
        }
        Set<String> result = ConcurrentHashMap.newKeySet();
        Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
        Set<String> newLayer = ConcurrentHashMap.newKeySet();
        Deque<String> queue = new ArrayDeque<>();
        ConcurrentHashMap<String, IOException> exceptions = new ConcurrentHashMap<>();
        Phaser phaser = new Phaser(1);

        queue.add(url);
        for (int i = depth; i >= 1; i--) {
            for (var link : queue) {
                downloadLinks(link, result, exceptions, visitedLinks, phaser, newLayer, i, needToAddHosts);
            }
            phaser.arriveAndAwaitAdvance();
            queue.clear();
            queue.addAll(newLayer);
            newLayer.clear();
        }
        return new Result(result.stream().toList(), exceptions);
    }

    private class HostWorker {
        // :NOTE: не нужно Concurrent
        //fixed
        private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
        private int working = 0;

        private synchronized void addTask(Runnable task) {
            if (working >= perHost) {
                queue.add(task);
            } else {
                downloadTreads.submit(task);
                working++;
            }
        }

        private synchronized void runTask() {
            if (queue.peek() != null) {
                downloadTreads.submit(queue.poll());
            } else {
                working--;
            }
        }
    }
}
