package io.axoniq.axonserver.eventstore.transformation.requestprocessor;

import io.axoniq.axonserver.eventstore.transformation.requestprocessor.EventStoreStateStore.EventStoreState;
import io.axoniq.axonserver.grpc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.function.Function;

import static java.lang.String.format;


/**
 * Implementation of {@link ContextTransformer} that guarantees that all actions are performed sequentially.
 */
public class SequentialContextTransformer implements ContextTransformer {

    private static final Logger logger = LoggerFactory.getLogger(SequentialContextTransformer.class);

    private final String context;
    private final ContextTransformationStore store;
    private final EventStoreStateStore eventStoreStateStore;
    private final TransformationStateConverter converter;

    private final Sinks.Many<Mono<?>> taskExecutor = Sinks.many()
                                                          .unicast()
                                                          .onBackpressureBuffer();


    public SequentialContextTransformer(String context,
                                        ContextTransformationStore store,
                                        EventStoreStateStore eventStoreStateStore,
                                        TransformationStateConverter converter) {
        this.context = context;
        this.store = store;
        this.eventStoreStateStore = eventStoreStateStore;
        this.converter = converter;
        startListening();
    }

    private void startListening() {
        taskExecutor.asFlux()
                    .concatMap(Function.identity())
                    .onErrorContinue((error, o) -> {})
                    .subscribe();
    }

    @Override
    public Mono<Void> start(String id, String description) { //todo check saving order
        return eventStoreStateStore.state(context)
                                   .flatMap(eventStoreState -> eventStoreState.transform(id))
                                   .flatMap(eventStoreStateStore::save)
                                   .then(store.create(id, description))
                                   .then() // TODO remove it
                                   .as(this::sequential);
    }

    @Override
    public Mono<Void> deleteEvent(String transformationId, long tokenToDelete, long sequence) {
        logger.info("Invoking delete event pipeline");
        return perform(transformationId,
                       "DELETE_EVENT",
                       transformation -> transformation.deleteEvent(tokenToDelete, sequence))
                .doFirst(() -> logger.info("Performing DELETE EVENT action."))
                .as(this::sequential);
    }

    @Override
    public Mono<Void> replaceEvent(String transformationId, long token, Event event, long sequence) {
        return perform(transformationId,
                       "REPLACE_EVENT",
                       transformation -> transformation.replaceEvent(token, event, sequence))
                .as(this::sequential);
    }

    @Override
    public Mono<Void> cancel(String transformationId) {
        return perform(transformationId,
                       "CANCEL",
                       Transformation::cancel)
                .then(eventStoreStateStore.state(context))
                .flatMap(EventStoreState::cancelled)
                .flatMap(eventStoreStateStore::save)
                .as(this::sequential);
    }


    @Override
    public Mono<Void> startApplying(String transformationId, long sequence, String applier) {
        return perform(transformationId,
                       "START_APPLYING_TRANSFORMATION",
                       transformation -> transformation.startApplying(sequence, applier))
                .as(this::sequential);
    }

    @Override
    public Mono<Void> markApplied(String transformationId) {
        return perform(transformationId,
                       "MARK_AS_APPLIED",
                       Transformation::markApplied)
                .then(eventStoreStateStore.state(context))
                .flatMap(EventStoreState::transformed)
                .flatMap(eventStoreStateStore::save)
                .as(this::sequential);
    }

    @Override
    public Mono<Void> markCompacted(String compactionId) {
        return eventStoreStateStore.state(context)
                                   .flatMap(EventStoreState::compacted)
                                   .flatMap(eventStoreStateStore::save)
                                   .as(this::sequential);
    }

    @Override
    public Mono<Void> compact(String compactionId) {
        return eventStoreStateStore.state(context)
                                   .flatMap(eventStoreState -> eventStoreState.compact(compactionId))
                                   .flatMap(eventStoreStateStore::save)
                                   .as(this::sequential);
    }

    private Mono<Void> perform(String transformationId,
                               String actionName,
                               Function<Transformation, Mono<TransformationState>> action) {
        return store.transformation(transformationId) // state interface (implemented by jpa) #with(...)
                    .flatMap(converter::from)
                    .checkpoint(format("Starting %s action", actionName))
                    .flatMap(action)
                    .checkpoint(format("Action %s completed", actionName))
                    .flatMap(store::save)
                    .checkpoint(format("Transformation updated after %s", actionName));
    }

    private <R> Mono<R> sequential(Mono<R> action) {// TODO: 09/11/2022 rethink sequentializing options
        return Mono.deferContextual(contextView -> {
            Sinks.One<R> actionResult = Sinks.one();
            while (taskExecutor.tryEmitNext(action.doOnError(t -> actionResult.emitError(t,
                                                                                         Sinks.EmitFailureHandler.FAIL_FAST))
                                                  .doOnSuccess(next -> actionResult.emitValue(next,
                                                                                              Sinks.EmitFailureHandler.FAIL_FAST)))
                    != Sinks.EmitResult.OK) {
            }
            return actionResult
                    .asMono()
                    .contextWrite(contextView);
        });
    }
}