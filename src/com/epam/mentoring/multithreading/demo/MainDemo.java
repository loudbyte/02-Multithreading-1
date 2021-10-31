package com.epam.mentoring.multithreading.demo;

import static com.epam.mentoring.multithreading.demo.MainDemo.CONSUMER_DURATION;
import static com.epam.mentoring.multithreading.demo.MainDemo.PRODUCER_DURATION;
import static com.epam.mentoring.multithreading.demo.MainDemo.WORKLOAD;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MainDemo {

  public static final String PRODUCER_DURATION = "producer duration";
  public static final String CONSUMER_DURATION = "consumer duration";
  public static final int ITERATIONS_FOR_AVERAGE = 10;
  public static final int WORKLOAD = 1000000;

  public static void main(String[] args) throws Exception {
    System.out.println("Start");
    System.out.println("Java version: " + System.getProperty("java.version"));

    long producerSumDuration = 0;
    long consumerSumDuration = 0;

    for (int i = 0; i < ITERATIONS_FOR_AVERAGE; i++) {
      System.out.print("\n-------Iteration #" + i);
      Task task = new Task();
      Map<String, Long> result = task.doTask();
      producerSumDuration += result.get(PRODUCER_DURATION);
      consumerSumDuration += result.get(CONSUMER_DURATION);
      while (!task.isTerminated()) {
      }
    }

    long producerAverageDuration = producerSumDuration / ITERATIONS_FOR_AVERAGE;
    long consumerAverageDuration = consumerSumDuration / ITERATIONS_FOR_AVERAGE;

    System.out.println("\n");
    System.out.println("Producer AVG: " + producerAverageDuration + " milli sec");
    System.out.println("Consumer AVG: " + consumerAverageDuration + " milli sec");

//    long producerAvgSec = TimeUnit.SECONDS.convert(producerAverageDuration, TimeUnit.MILLISECONDS);
//    long consumerAvgSec = TimeUnit.SECONDS.convert(consumerAverageDuration, TimeUnit.MILLISECONDS);
//
//    System.out.println("Producer AVG: " + producerAvgSec + " sec");
//    System.out.println("Consumer AVG: " + consumerAvgSec + " sec");
  }
}

class Task {

  private long numberOfIterations = 0;
  private final ExecutorService executorService = Executors.newFixedThreadPool(2);
  private  final ConcurrentHashMap<Integer, Integer> integerIntegerHashMap = new ConcurrentHashMap<>();
  private final Object lock = new Object();

  public Map<String, Long> doTask() throws Exception {

    Callable<Long> producer = () -> {
      long startTime = System.nanoTime();

//      System.out.print("  start producer ");
      produce(integerIntegerHashMap, lock);
//      System.out.print("  end producer ");

      long endTime = System.nanoTime();
      long duration = endTime - startTime;
      return TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
    };

    Callable<Long> consumer = () -> {
      long startTime = System.nanoTime();

//      System.out.print("  start consumer ");
      consume(integerIntegerHashMap, lock);
//      System.out.print("  end consumer ");

      long endTime = System.nanoTime();
      long duration = endTime - startTime;
      return TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
    };

    Future<Long> producerFuture = executorService.submit(producer);
    Future<Long> consumerFuture = executorService.submit(consumer);
    Long producerDuration = producerFuture.get();
    Long consumerDuration = consumerFuture.get();
    HashMap<String, Long> durations = new HashMap<>();
    durations.put(PRODUCER_DURATION, producerDuration);
    durations.put(CONSUMER_DURATION, consumerDuration);
    executorService.shutdown();
    executorService.awaitTermination(120, TimeUnit.SECONDS);
    System.out.print(" Number of consumer iterations = " + numberOfIterations
        + ", workload = " + WORKLOAD + ", isEqual: " + (numberOfIterations == WORKLOAD));
    return durations;
  }

  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  private void produce(ConcurrentHashMap<Integer, Integer> integerIntegerHashMap, Object lock) {

    for (int i = 0; i < WORKLOAD; i++) {
      synchronized (lock) {
        Random random = new Random();
        int randomInt = random.nextInt();
        randomInt = randomInt > 0 ? randomInt : -randomInt;
        integerIntegerHashMap.put(i, randomInt);
        lock.notifyAll();
      }
    }
  }

  private void consume(ConcurrentHashMap<Integer, Integer> integerIntegerHashMap, Object lock) {

    BigInteger sum = new BigInteger("0");

    do {
      for (Entry<Integer, Integer> entry : integerIntegerHashMap.entrySet()) {
        synchronized (lock) {
          sum = sum.add(new BigInteger(entry.getValue().toString()));
          integerIntegerHashMap.remove(entry.getKey());
          numberOfIterations++;
          lock.notifyAll();
        }
      }
    } while (numberOfIterations != WORKLOAD);
  }
}