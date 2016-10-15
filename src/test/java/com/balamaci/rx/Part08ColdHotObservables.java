package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class explains the difference between the cold observables and hot observables
 *
 * Cold Observables generate notifications for each subscriber and hot Observables
 * are always running, broadcasting notifications to all of their subscribers.
 * Think of a hot Observable as a radio station. All of the listeners that are listening to it
 * at this moment listen to the same song. A cold Observable is a music CD. Many people
 * can buy it and listen to it independently
 *
 * (Best Definition by Nickolay Tsvetinov, author of Learning Reactive Programming with Java 8)
 *
 * author chintoo created on 10/15/16.
 */
public class Part08ColdHotObservables implements BaseTestObservables {

    /**
     * Hot Observables can be used for different purposes and be created in many ways.
     * Sometimes you may want to convert a cold Observable into a hot one, perhaps because
     * the emissions are expensive to replay. You can turn a cold Observable into a hot one
     * is by making it a ConnectableObservable. Call any Observable's publish() method, set up
     * its Subscribers, and then call connect() to fire the emissions hotly all at once.
     */
    @Test
    public void hotObservables() {
        Observable<String> coldObservables =
            Observable.just("Alpha", "Beta", "Gamma", "Delta", "Epsilon");

        ConnectableObservable<String> hotObservables =  coldObservables.publish();
        subscribeWithLog(hotObservables);

        subscribeWithLog(hotObservables.map(s -> s.length()));

        hotObservables.connect();

    }

    @Test
    public void hotColdObservables() {
        ConnectableObservable<Long> timer =
            Observable.interval(1, TimeUnit.SECONDS).publish();

        subscribeWithLog(timer);

        timer.connect();

        Helpers.sleepMillis(5000);

        subscribeWithLog(timer.map(i -> i * 1000));

        Helpers.sleepMillis(5000);
    }

    /**
     * You can mix cold and hot Observables together to compose events and data in a single stream.
     * For example, you can take a hot Observable, then flatMap() it to a cold Observable to push
     * a List<Integer> every second.
     */
    @Test
    public void composingHotColdObservables() {
        Observable<String> items = Observable.just("Alpha","Beta","Gamma","Delta","Epsilon");

        ConnectableObservable<Long> timer =
            Observable.interval(1,TimeUnit.SECONDS).publish();

        Observable<List<Integer>> listObservable =
            timer.flatMap(i -> items.map(s -> s.length()).toList());

        subscribeWithLog(listObservable);

        timer.connect();

        Helpers.sleepMillis(5000);
    }
}
