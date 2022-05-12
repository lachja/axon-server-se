/*
 *  Copyright (c) 2017-2021 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 *  under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.eventstore.transformation.requestprocessor;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Stores the information on each transformation.
 * @author Marc Gathier
 * @since 4.6.0
 */
@Entity
@Table(name = "event_store_transformations")
public class EventStoreTransformationJpa {

    public enum Status {
        ACTIVE,
        CANCELLING,
        CANCELLED, // final state
        APPLYING,
        APPLIED, // final state
        ROLLING_BACK,
        ROLLED_BACK, // final state
        FAILED // final state
    }

    @Id
    private String transformationId;
    private String context;

    @Enumerated(EnumType.ORDINAL)
    private Status status;

    private int version;

    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateApplied;

    private String appliedBy;

    // TODO: 12/28/21 name
    private Long lastSequence;

    private Long lastEventToken;

    public EventStoreTransformationJpa() {
    }

    public EventStoreTransformationJpa(String transformationId, String context, int version) {
        this.transformationId = transformationId;
        this.context = context;
        this.version = version;
        this.status = Status.ACTIVE;
    }

    public EventStoreTransformationJpa(EventStoreTransformationJpa original)  {
        this.transformationId = original.transformationId;
        this.appliedBy = original.appliedBy;
        this.context = original.context;
        this.dateApplied = original.dateApplied;
        this.description = original.description;
        this.lastSequence = original.lastSequence;
        this.lastEventToken = original.lastEventToken;
        this.status = original.status;
        this.version = original.version;
    }

    public String getTransformationId() {
        return transformationId;
    }

    public void setTransformationId(String transformationId) {
        this.transformationId = transformationId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateApplied() {
        return dateApplied;
    }

    public void setDateApplied(Date dateApplied) {
        this.dateApplied = dateApplied;
    }

    public String getAppliedBy() {
        return appliedBy;
    }

    public void setAppliedBy(String appliedBy) {
        this.appliedBy = appliedBy;
    }

    public Long getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(Long lastTransformationEntrySequence) {
        this.lastSequence = lastTransformationEntrySequence;
    }

    public Long getLastEventToken() {
        return lastEventToken;
    }

    public void setLastEventToken(Long lastEventToken) {
        this.lastEventToken = lastEventToken;
    }

    @Override
    public String toString() {
        return "EventStoreTransformationJpa{" +
                "transformationId='" + transformationId + '\'' +
                ", context='" + context + '\'' +
                ", status=" + status +
                ", version=" + version +
                ", description='" + description + '\'' +
                ", dateApplied=" + dateApplied +
                ", appliedBy='" + appliedBy + '\'' +
                ", lastSequence=" + lastSequence +
                ", lastEventToken=" + lastEventToken +
                '}';
    }
}
