package io.slingr.services.services.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Service to manage threads
 *
 */
public class ConcurrencyService {
    private final Logger logger = LoggerFactory.getLogger(ConcurrencyService.class);

    private final static int MAX_THREADS = 10;
    private final static int SLEEP_TIME = 100;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
    private volatile boolean running = Boolean.FALSE;
    private final Queue<Task> pendingTasks = new LinkedList<>();

    public void start() {
        running = true;
        logger.info("Starting services concurrency service");
        while (running) {
            Task task = pendingTasks.poll();
            if (task != null) {
                if (((ThreadPoolExecutor) executor).getActiveCount() < MAX_THREADS) {
                    executor.execute(task);
                } else {
                    pendingTasks.add(task);
                }
            } else {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    public void stop() {
        logger.info("Stopping services concurrency service");
        running = false;
        pendingTasks.clear();
        executor.shutdown();
    }

    public void queueTask(Task task) {
        pendingTasks.add(task);
    }

    public interface Task extends Runnable {

    }
}