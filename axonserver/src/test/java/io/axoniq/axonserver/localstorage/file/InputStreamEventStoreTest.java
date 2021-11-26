/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.localstorage.file;

import io.axoniq.axonserver.config.SystemInfoProvider;
import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.localstorage.EventTransformationResult;
import io.axoniq.axonserver.localstorage.EventType;
import io.axoniq.axonserver.localstorage.EventTypeContext;
import io.axoniq.axonserver.localstorage.SerializedEvent;
import io.axoniq.axonserver.localstorage.SerializedTransactionWithToken;
import io.axoniq.axonserver.localstorage.transformation.DefaultEventTransformerFactory;
import io.axoniq.axonserver.localstorage.transformation.EventTransformerFactory;
import io.axoniq.axonserver.metric.DefaultMetricCollector;
import io.axoniq.axonserver.metric.MeterFactory;
import io.axoniq.axonserver.test.TestUtils;
import io.axoniq.axonserver.topology.Topology;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.*;

import java.util.SortedSet;

import static junit.framework.TestCase.*;

/**
 * @author Marc Gathier
 */
public class InputStreamEventStoreTest {
    private InputStreamEventStore testSubject;

    @Before
    public void setUp() {
        EmbeddedDBProperties embeddedDBProperties = new EmbeddedDBProperties(new SystemInfoProvider() {
        });
        embeddedDBProperties.getEvent().setStorage(TestUtils
                                                           .fixPathOnWindows(InputStreamEventStore.class
                                                                                     .getResource("/data").getFile()));
        embeddedDBProperties.getEvent().setForceCleanMmapIndex(true);
        embeddedDBProperties.getEvent().setUseMmapIndex(true);
        String context = Topology.DEFAULT_CONTEXT;
        MeterFactory meterFactory = new MeterFactory(new SimpleMeterRegistry(), new DefaultMetricCollector());

        StandardIndexManager indexManager = new StandardIndexManager(context, embeddedDBProperties.getEvent(),
                                                                     EventType.EVENT,
                                                                     meterFactory);
        indexManager.init();
        EventTransformerFactory eventTransformerFactory = new DefaultEventTransformerFactory();
        testSubject = new InputStreamEventStore(new EventTypeContext(context, EventType.EVENT), indexManager,
                                                eventTransformerFactory,
                                                embeddedDBProperties.getEvent(), meterFactory);
        testSubject.init(true);
    }




    @Test
    public void getEventSource() {
        EventSource eventSource = testSubject.getEventSource(0).get();
        try (EventIterator iterator = eventSource.createEventIterator(0, 5)) {
            assertTrue(iterator.hasNext());
            EventInformation next = iterator.next();
            assertEquals(5, next.getToken());
            while (iterator.hasNext()) {
                next = iterator.next();
                System.out.println(next.getPosition());
            }
            assertEquals(13, next.getToken());
        }
    }

    @Test
    public void readBackwards() {
        EventSource eventSource = testSubject.getEventSource(0).get();
        int[] positions = {432, 502, 572, 642, 712, 782, 852, 922};
        for (int i = positions.length - 1; i >= 0; i--) {
            eventSource.readEvent(positions[i]);
        }
        eventSource.close();
    }

    @Test
    public void iterateTransactions() {
        EventSource eventSource = testSubject.getEventSource(0).get();
        TransactionIterator iterator = eventSource.createTransactionIterator(0, 5, true);
        assertTrue(iterator.hasNext());
        SerializedTransactionWithToken next = iterator.next();
        assertEquals(5, next.getToken());
        while (iterator.hasNext()) {
            next = iterator.next();
        }
        assertEquals(13, next.getToken());
    }

    @Test
    public void getSegments() {
        SortedSet<Long> segments = testSubject.getSegments();
        assertTrue(segments.contains(0L));
        assertTrue(segments.contains(14L));
        assertEquals(14, (long)segments.first());
    }

    @Test
    public void transform() {
        testSubject.transformContents(0, Long.MAX_VALUE, false, 1, (event, token) -> {
            System.out.println("id=" + event.getAggregateIdentifier());
            if (event.getAggregateIdentifier().equals("abb070e9-943f-4947-8def-c50481b968c7")) {
                return new EventTransformationResult() {

                    @Override
                    public Event event() {
                        return Event.getDefaultInstance();
                    }

                    @Override
                    public long nextToken() {
                        return token + 1;
                    }
                };
            }
            return new EventTransformationResult() {

                @Override
                public Event event() {
                    return event;
                }

                @Override
                public long nextToken() {
                    return token + 1;
                }
            };
        }, transformationProgress -> {});

        SerializedEvent event = testSubject.eventsPerAggregate(
                "abb070e9-943f-4947-8def-c50481b968c7",
                0,
                Long.MAX_VALUE,
                0).blockLast();
        assertNull(event);
    }
}