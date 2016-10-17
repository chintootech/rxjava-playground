package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * author chintoo created on 10/15/16.
 */
public class Part09ParallelOperation implements BaseTestObservables {

    @Test
    public void serialOperations() {
        Observable<Integer> vals = Observable.range(1,10);

        vals.subscribeOn(Schedulers.computation())
            .map(Part09ParallelOperation::intenseCalculation)
            .map(Object::toString)
            .subscribe(getLogSubscriber());

        Helpers.sleepMillis(20000);
    }

    @Test
    public void parallelOperation() {

        Observable<Integer> vals = Observable.range(1,10);

        vals
            .flatMap(val -> Observable.just(val)
                .subscribeOn(Schedulers.computation())
                .map(Part09ParallelOperation::intenseCalculation)
            )
            .map(Object::toString)
            .subscribe(getLogSubscriber());
        Helpers.sleepMillis(20000);
    }

    @Test
    public void parallelOperationUsingCustomExecutor() {
        int threadCt = Runtime.getRuntime().availableProcessors() + 1;

        ExecutorService executor = Executors.newFixedThreadPool(threadCt);
        Scheduler scheduler = Schedulers.from(executor);

        Observable<Integer> vals = Observable.range(1,10);

        vals
            .flatMap(val -> Observable.just(val)
                .subscribeOn(scheduler)
                .map(Part09ParallelOperation::intenseCalculation)
        ).map(Object::toString)
         .subscribe(getLogSubscriber());

        Helpers.sleepMillis(20000);
    }

    @Test
    public void anotherParallelOperation() {

        Observable<Integer> vals = Observable.range(1,10);

        vals.flatMap(val -> Observable.just(val)
            .subscribeOn(Schedulers.computation())
            .map(Part09ParallelOperation::intenseCalculation)
        ).toList()
            .subscribe(val -> System.out.println("Subscriber received "
            + val + " on "
            + Thread.currentThread().getName()));

        Helpers.sleepMillis(20000);
    }

    private static int intenseCalculation(int i) {
        try {
            log.info("Calculating {} on {}", i, Thread.currentThread().getName());
            Thread.sleep(Helpers.randInt(1000,5000));
            return i;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
