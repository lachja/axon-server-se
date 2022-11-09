package io.axoniq.axonserver.eventstore.transformation.requestprocessor;

import io.axoniq.axonserver.eventstore.transformation.transformation.ActiveTransformation;
import io.axoniq.axonserver.eventstore.transformation.transformation.ApplyingTransformation;
import io.axoniq.axonserver.eventstore.transformation.transformation.CancellingTransformation;
import io.axoniq.axonserver.eventstore.transformation.transformation.FinalTransformation;
import io.axoniq.axonserver.eventstore.transformation.transformation.active.TransformationResources;
import reactor.core.publisher.Mono;

public class ContextTransformationStateConverter implements TransformationStateConverter {

    private final EventProvider eventProvider;

    public ContextTransformationStateConverter(EventProvider eventProvider) {
        this.eventProvider = eventProvider;
    }

    @Override
    public Mono<Transformation> from(TransformationState entity) {
        return Mono.fromSupplier(() -> {
            switch (entity.status()) {
                case ACTIVE:
                    return activeTransformation(entity);
                case APPLYING:
                    return applyingTransformation(entity);
                case CANCELLING:
                    return cancellingTransformation(entity);
                case APPLIED:
                case CANCELLED:
                case FAILED:
                    return finalTransformation();
                default:
                    throw new IllegalStateException("Unexpected transformation status.");
            }
        });
    }

    private ActiveTransformation activeTransformation(TransformationState state) {
        return new ActiveTransformation(resources(), state);
    }

    private  ApplyingTransformation applyingTransformation(TransformationState state) {
        return new ApplyingTransformation(state);
    }

    private CancellingTransformation cancellingTransformation(TransformationState state) {
        return new CancellingTransformation(state);
    }

    private FinalTransformation finalTransformation() {
        return new FinalTransformation();
    }

    private TransformationResources resources() {
        return new ContextTransformationResources(eventProvider);
    }
}
