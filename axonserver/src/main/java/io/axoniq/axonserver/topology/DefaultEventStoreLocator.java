/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.topology;

import io.axoniq.axonserver.eventstore.transformation.impl.TransformationProcessor;
import io.axoniq.axonserver.exception.ErrorCode;
import io.axoniq.axonserver.exception.MessagingPlatformException;
import io.axoniq.axonserver.localstorage.LocalEventStore;
import io.axoniq.axonserver.message.event.EventStore;

import javax.annotation.PostConstruct;

/**
 * Default implementation for an EventStoreLocator.
 *
 * @author Marc Gathier
 * @since 4.0
 */
public class DefaultEventStoreLocator implements EventStoreLocator {

    private final LocalEventStore localEventStore;
    private final TransformationProcessor transformationProcessor;


    public DefaultEventStoreLocator(LocalEventStore localEventStore,
                                    TransformationProcessor transformationProcessor) {
        this.localEventStore = localEventStore;
        this.transformationProcessor = transformationProcessor;
    }

    @PostConstruct
    public void init() {
        localEventStore.initContext(Topology.DEFAULT_CONTEXT, false);
        transformationProcessor.restartApply(Topology.DEFAULT_CONTEXT)
                .thenAccept(transformationId -> {
                    if( transformationId != null) {
                        transformationProcessor.complete(transformationId);
                    }
                });
    }

    @Override
    public EventStore getEventStore(String context) {
        if (Topology.DEFAULT_CONTEXT.equals(context)) {
            return localEventStore;
        }
        throw new MessagingPlatformException(ErrorCode.NO_EVENTSTORE, "No eventstore found");
    }
}
