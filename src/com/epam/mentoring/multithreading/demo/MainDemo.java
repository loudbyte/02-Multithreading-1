package com.epam.mentoring.multithreading.demo;

import static com.epam.mentoring.multithreading.demo.MainDemo.CONSUMER_DURATION;
import static com.epam.mentoring.multithreading.demo.MainDemo.PRODUCER_DURATION;
import static com.epam.mentoring.multithreading.demo.MainDemo.WORKLOAD;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
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
    System.out.println("Workload: " + WORKLOAD);
    System.out.println("Iterations: " + ITERATIONS_FOR_AVERAGE);

    long producerSumDuration = 0;
    long consumerSumDuration = 0;

    for (int i = 0; i < ITERATIONS_FOR_AVERAGE; i++) {
//      System.out.print("\n Iteration #" + i);
      Task task = new Task();
      Map<String, Long> result = task.doTask();
      producerSumDuration += result.get(PRODUCER_DURATION);
      consumerSumDuration += result.get(CONSUMER_DURATION);
      while (!task.isTerminated()) {
      }
    }

    long producerAverageDuration = producerSumDuration / ITERATIONS_FOR_AVERAGE;
    long consumerAverageDuration = consumerSumDuration / ITERATIONS_FOR_AVERAGE;

    System.out.println("");
    System.out.println("Producer average duration: " + producerAverageDuration + " milli sec");
    System.out.println("Consumer average duration: " + consumerAverageDuration + " milli sec");

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
  private final Map<Integer, Integer> integerIntegerHashMap = new ThreadSaveMap<Integer, Integer>();

  public Map<String, Long> doTask() throws Exception {

    Callable<Long> producer = new Callable<Long>() {
      @Override
      public Long call() {
        long startTime = System.nanoTime();

//      System.out.print("  start producer ");
        produce(integerIntegerHashMap);
//      System.out.print("  end producer ");

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        return TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
      }
    };

    Callable<Long> consumer = new Callable<Long>() {
      @Override
      public Long call() {
        long startTime = System.nanoTime();

//      System.out.print("  start consumer ");
        consume(integerIntegerHashMap);
//      System.out.print("  end consumer ");

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        return TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
      }
    };

    Future<Long> producerFuture = executorService.submit(producer);
    Future<Long> consumerFuture = executorService.submit(consumer);
    Long producerDuration = producerFuture.get();
    Long consumerDuration = consumerFuture.get();
    HashMap<String, Long> durations = new HashMap<String, Long>();
    durations.put(PRODUCER_DURATION, producerDuration);
    durations.put(CONSUMER_DURATION, consumerDuration);
    executorService.shutdown();
    executorService.awaitTermination(120, TimeUnit.SECONDS);
//    System.out.print(" Number of consumer iterations = " + numberOfIterations
//        + ", workload = " + WORKLOAD + ", isEqual: " + (numberOfIterations == WORKLOAD));
    if (numberOfIterations != WORKLOAD) {
      throw new Exception("Number of consumer iterations ("+numberOfIterations+") is not equal workload("+WORKLOAD+ ").");
    }
    return durations;
  }

  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  private void produce(Map<Integer, Integer> integerIntegerHashMap) {

    for (int i = 0; i < WORKLOAD; i++) {
      Random random = new Random();
      int randomInt = random.nextInt();
      randomInt = randomInt > 0 ? randomInt : -randomInt;
      integerIntegerHashMap.put(i, randomInt);
    }
  }

  private void consume(Map<Integer, Integer> integerIntegerHashMap) {

    BigInteger sum = new BigInteger("0");

    do {
      for (Entry<Integer, Integer> entry : integerIntegerHashMap.entrySet()) {
        sum = sum.add(new BigInteger(entry.getValue().toString()));
        integerIntegerHashMap.get(entry.getKey());
        integerIntegerHashMap.remove(entry.getKey());
        numberOfIterations++;
      }
    } while (numberOfIterations != WORKLOAD);
  }
}

class ThreadSaveMap<K, V> extends ConcurrentHashMap<K, V> {

  private final Object lock = new Object();

  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> result = new HashSet<Entry<K,V>>();
    for (Entry<K, V> entry : super.entrySet()) {
      synchronized (lock) {
        result.add(entry);
        lock.notifyAll();
      }
    }
    return result;
  }

  public V put(K key, V value) {
    synchronized (lock) {
      V put = super.put(key, value);
      lock.notifyAll();
      return put;
    }
  }

  public V remove(Object key) {
    synchronized (lock) {
      return super.remove(key);
    }
  }
}