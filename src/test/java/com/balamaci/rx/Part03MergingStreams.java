package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import javafx.util.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Operators for working with multiple streams
 *
 * @author sbalamaci
 */
public class Part03MergingStreams implements BaseTestObservables {

    /**
     * Zip operator operates sort of like a zipper in the sense that it takes an event from one stream and waits
     * for an event from another other stream. Once an event for the other stream arrives, it uses the zip function
     * to merge the two events.
     * This is an useful scenario when for example you want to make requests to remote services in parallel and
     * wait for the response before continuing
     *
     */
    @Test
    public void zipUsedToSlowDown() {
        CountDownLatch latch = new CountDownLatch(1);

        Observable<Boolean> isBlockedStream = Observable.from(CompletableFuture.supplyAsync(() -> {
                    Helpers.sleepMillis(200);
                    return Boolean.FALSE;
                }));
        Observable<Integer> creditScoreStream = Observable.from(CompletableFuture.supplyAsync(() -> {
                    Helpers.sleepMillis(2300);
                    return 200;
                }));

        Observable<Pair<Boolean, Integer>> periodicEmitter = Observable.zip(isBlockedStream, creditScoreStream,
                Pair::new);
        subscribeWithLog(periodicEmitter, latch);

        Helpers.wait(latch);
    }

    /**
     * Some other useful usecase it's to s
     */
    @Test
    public void zipUsedToSlowDownAnotherStream() {
        CountDownLatch latch = new CountDownLatch(1);

        Observable<String> colors = Observable.just("red", "green", "blue");
        Observable<Long> timer = Observable.interval(2, TimeUnit.SECONDS);

        Observable<String> periodicEmitter = Observable.zip(colors, timer, (key, val) -> key);
        subscribeWithLog(periodicEmitter, latch);

        Helpers.wait(latch);
    }



    @Test
    public void mergeOperator() {
        Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(2);

        BlockingObservable observable = Observable.merge(colors, numbers).toBlocking();
        subscribeWithLog(observable);
    }


    @Test
    public void concatStreams() {
        Observable<String> colors = periodicEmitter("red", "green", "blue", 2, TimeUnit.SECONDS);

        Observable<Long> numbers = Observable.interval(1, TimeUnit.SECONDS)
                .take(4);

        BlockingObservable observable = Observable.concat(colors, numbers).toBlocking();
        subscribeWithLog(observable);
    }


}
