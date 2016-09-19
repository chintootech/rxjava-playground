package com.balamaci.rx;

import com.balamaci.rx.util.Helpers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author sbalamaci
 */
public class Part01CreateObservable implements BaseTestObservables {

    private static final Logger log = LoggerFactory.getLogger(Part01CreateObservable.class);


    @Test
    public void just() {
        Observable<Integer> observable = Observable.just(1, 5, 10);

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val));
    }

    @Test
    public void range() {
        Observable<Integer> observable = Observable.range(1, 10);

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val));
    }

    @Test
    public void fromArray() {
        Observable<String> observable = Observable.from(new String[]{"red", "green", "blue", "black"});

        observable.subscribe(
                val -> log.info("Subscriber received: {}"));
    }

    /**
     * We can also create an Observable from Future, making easier to switch from legacy code to reactive
     */
    @Test
    public void fromFuture() {
        CompletableFuture<String> completableFuture = CompletableFuture.
                supplyAsync(() -> { //starts a background thread the ForkJoin common pool
                      Helpers.sleepMillis(100);
                      return "red";
                });

        Observable<String> observable = Observable.from(completableFuture);
        observable.subscribe(val -> log.info("Subscriber received: {}", val));


        completableFuture = CompletableFuture.completedFuture("green");
        observable = Observable.from(completableFuture);
        observable.subscribe(val -> log.info("Subscriber2 received: {}", val));
    }

    /**
     * We can also create an Observable from Callable, making easier to switch from legacy code to
     * reactive.
     *
     * The emission of value to the subscriber using onNext and either onCompleted or onError
     * is handled by the library.
     */
    @Test
    public void createUsingFromCallable() throws Exception {
        TestSubscriber<String> testSubscriber = TestSubscriber.create();

        Observable.fromCallable(() -> {
            Thread.sleep(2_000);
            log.info("Finished the callable");
            return "done";
        })//.subscribeOn(Schedulers.newThread())
            .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent(2_500, MILLISECONDS);
        testSubscriber.assertCompleted();
        testSubscriber.assertValueCount(1);
        testSubscriber.assertValue("done");
    }


    @Test
    public void createAnObservableThatEmitsInAnInterval() throws Exception {
        TestSubscriber<Long> testSubscriber = TestSubscriber.create();

        Observable.interval(1, 500, MILLISECONDS).take(2_750, MILLISECONDS).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent(3_000, MILLISECONDS);
        testSubscriber.assertCompleted();
        testSubscriber.assertValueCount(6);
    }

    /**
     * Using Observable.create to handle the actual emissions of events with the events like onNext, onCompleted, onError
     * <p>
     * When subscribing to the Observable with observable.subscribe() the lambda code inside create() gets executed.
     * Observable.subscribe can take 3 handlers for each type of event - onNext, onError and onCompleted
     * <p>
     * When using Observable.create you need to be aware of <b>Backpressure</b> and that Observables based on 'create' method
     * are not Backpressure aware {@see Part07BackpressureHandling}.
     */
    @Test
    public void createSimpleObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.onNext(1);

            log.info("Emitting 2nd");
            subscriber.onNext(2);

            subscriber.onCompleted();
        });

        log.info("Subscribing");
        Subscription subscription = observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event"));
    }

    /**
     * Observable emits an Error event which is a terminal operation and the subscriber is no longer executing
     * it's onNext callback. We're actually breaking the the Observable contract that we're still emitting events
     * after onComplete or onError have fired.
     */
    @Test
    public void createSimpleObservableThatEmitsError() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.onNext(1);

            subscriber.onError(new RuntimeException("Test exception"));

            log.info("Emitting 2nd");
            subscriber.onNext(2);
        });

        observable.subscribe(
                val -> log.info("Subscriber received: {}", val),
                err -> log.error("Subscriber received error", err),
                () -> log.info("Subscriber got Completed event")
        );
    }

    /**
     * Observables are lazy meaning that the code inside create() doesn't get executed without subscribing to the Observable
     * So event if we sleep for a long time inside create() method(to simulate a costly operation),
     * without subscribing to this Observable the code is not executed and the method returns immediately.
     */
    @Test
    public void observablesAreLazy() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting but sleeping for 5 secs"); //this is not executed
            Helpers.sleepMillis(5000);
            subscriber.onNext(1);
        });
        log.info("Finished");
    }

    /**
     * When subscribing to an Observable, the create() method gets executed for each subscription
     * this means that the events inside create are re-emitted to each subscriber. So every subscriber will get the
     * same events and will not lose any events.
     */
    @Test
    public void multipleSubscriptionsToSameObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st event");
            subscriber.onNext(1);

            log.info("Emitting 2nd event");
            subscriber.onNext(2);

            subscriber.onCompleted();
        });

        log.info("Subscribing 1st subscriber");
        observable.subscribe(val -> log.info("First Subscriber received: {}", val));

        log.info("=======================");

        log.info("Subscribing 2nd subscriber");
        observable.subscribe(val -> log.info("Second Subscriber received: {}", val));
    }

    /**
     * Inside the create() method, we can check is there are still active subscribers to our Observable.
     * It's a way to prevent to do extra work(like for ex. querying a datasource for entries) if no one is listening
     * In the following example we'd expect to have an infinite stream, but because we stop if there are no active
     * subscribers we stop producing events.
     * The **take()** operator unsubscribes from the Observable after it's received the specified amount of events
     */
    @Test
    public void showUnsubscribeObservable() {
        Observable<Integer> observable = Observable.create(subscriber -> {

            int i = 1;
            while(true) {
                if(subscriber.isUnsubscribed()) {
                    break;
                }

                subscriber.onNext(i++);
            }
            //subscriber.onCompleted(); too late to emit Complete event since subscriber already unsubscribed
        });

        observable
                .take(5)
                .subscribe(
                        val -> log.info("Subscriber received: {}", val),
                        err -> log.error("Subscriber received error", err),
                        () -> log.info("Subscriber got Completed event") //The Complete event is triggered by 'take()' operator
        );
    }

    /**
     * In this examples, the following are worth noticing:
     * <li>
     * 1) The Action - The create method receives an implementation of Observable.OnSubscribe
     *    interface. This implementation defines what action will be taken when a subscriber
     *    subscribes to the Observable.
     * 2) The Action is lazy - The call method is called by library each time a subscriber
     *    subscribes to the Observable. Till then the action is not executed, i.e. it is lazy.
     * 3) Events are pushed to subscriber - onNext, onError and onCompleted methods on Subscriber
     *    are used to push the events onto it. As per Rx Design Guidelines, the events pushed
     *    from Observable should follow the below rules:
     *      A) Zero, one or more than one calls to onNext
     *      B) Zero or only one call to either of onCompleted or onError
     * </li>
     */
    @Test
    public void createAnObservableUsingCreate() throws Exception {
        TestSubscriber<Integer> testSubscriber = TestSubscriber.create();

        Observable.create(new Observable.OnSubscribe<Integer>() {

            public void call(Subscriber<? super Integer> subscriber) {
                try {
                    for (int i = 0; i < 5; i++) {
                        if (subscriber.isUnsubscribed())
                            return;
                        int result = doSomeTimeTakingIoOperation(i);
                        log.info("Value received is {}", result);

                        if (subscriber.isUnsubscribed())
                            return;
                        subscriber.onNext(result); // Pass on the data to subscriber
                    }

                    if (subscriber.isUnsubscribed())
                        return;

                    subscriber.onCompleted(); // Signal about the completion subscriber
                } catch (Exception e) {
                    subscriber.onError(e); // Signal about the error to subscriber
                }
            }

            private int doSomeTimeTakingIoOperation(int i) {
                Helpers.sleepMillis(1_000);
                return i;
            }
        }).subscribeOn(Schedulers.io()).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEventAndUnsubscribeOnTimeout(2_500, MILLISECONDS);
        testSubscriber.assertNotCompleted();
        testSubscriber.assertValueCount(2);
    }

//    @Test
//    public

}
