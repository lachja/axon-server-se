/*
 * Copyright (c) 2017-2022 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.localstorage.transformation;

import io.axoniq.axonserver.grpc.event.EventWithToken;
import io.axoniq.axonserver.localstorage.file.TransformationProgress;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Operations to transform events in an event store.
 *
 * @author Marc Gathier
 * @since 4.6.0
 */
public interface LocalEventStoreTransformer {

    /**
     * Transforms events in the event store. Returns a {@link Flux} of progress objects where an object is published
     * when the transformation function reaches a certain milestone. The flux is completed when the transformation is
     * completed.
     *
     * @param context           the name of the context for the event store
     * @param version           the new version number for the event store
     * @param transformedEvents a {@link Flux} of transformed events
     * @return a flux of progress information objects
     */
    Flux<TransformationProgress> transformEvents(String context, int version, Flux<EventWithToken> transformedEvents);

    /**
     * Removes the events with given version, making the previous version of the events the active version.
     *
     * @param context the name of the context
     * @param version the version of the event store to rollback
     */
    Mono<Void> rollback(String context, int version);

    /**
     * Deletes all events in the event store related to the given context.
     *
     * @param context the name of the context
     */
    Mono<Void> compact(String context);

}
