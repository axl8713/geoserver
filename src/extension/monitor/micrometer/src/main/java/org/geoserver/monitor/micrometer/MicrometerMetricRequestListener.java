package org.geoserver.monitor.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataListener;
import org.geotools.util.logging.Logging;

public class MicrometerMetricRequestListener implements RequestDataListener {

    private static final Logger LOGGER = Logging.getLogger(MicrometerMetricRequestListener.class);
    private static final String MICROMETER = "micrometer";

    MonitorConfig config;
    PrometheusMeterRegistry registry;

    private final AtomicInteger requestsCounter = new AtomicInteger(0);

    /** Timer to record the total of a request from {@link RequestData#getTotalTime()}. */
    private final Meter.MeterProvider<Timer> requestTimer;

    /** Meter to record the response length of a request from {@link RequestData#getResponseLength()}. */
    private final Meter.MeterProvider<DistributionSummary> responseLengthSummary;

    /**
     * Timer to record the processing time of a request summing the values from
     * {@link RequestData#getResourcesProcessingTime()}.
     */
    private final Meter.MeterProvider<Timer> requestProcessingTimer;

    /** Timer to record the labeling time of a request from {@link RequestData#getLabellingProcessingTime()}. */
    private final Meter.MeterProvider<Timer> requestLabellingProcessingTimer;

    /**
     * Meter to record the remote address of the host that makes the request from {@link RequestData#getRemoteAddr()}.
     */
    private final Meter.MeterProvider<Counter> hostCounter;

    public MicrometerMetricRequestListener(MonitorConfig config, PrometheusMeterRegistry registry) {
        this.config = config;
        this.registry = registry;

        requestTimer = Timer.builder("requests.total").withRegistry(registry);
        requestProcessingTimer = Timer.builder("requests.processing").withRegistry(registry);
        requestLabellingProcessingTimer =
                Timer.builder("requests.labelling.processing").withRegistry(registry);
        responseLengthSummary = DistributionSummary.builder("requests.response.length")
                .baseUnit("bytes")
                .withRegistry(registry);
        hostCounter = Counter.builder("requests.host").withRegistry(registry);
    }

    @Override
    public void requestPostProcessed(RequestData rd) {
        try {
            if (!isMetricRequestEnabled()) {
                return;
            }

            if (rd == null) {
                return;
            }

            resetMetricIfNeeded();

            Tags tags = composeTags(rd);

            requestTimer.withTags(tags).record(rd.getTotalTime(), TimeUnit.MILLISECONDS);

            responseLengthSummary.withTags(tags).record(rd.getResponseLength());

            List<Long> resourcesProcessingTime = rd.getResourcesProcessingTime();
            if (resourcesProcessingTime != null) {
                requestProcessingTimer
                        .withTags(tags)
                        .record(resourcesProcessingTime.stream().reduce(0L, Long::sum), TimeUnit.MILLISECONDS);
            }

            Long labellingProcessingTime = rd.getLabellingProcessingTime();
            if (labellingProcessingTime != null) {
                requestLabellingProcessingTimer.withTags(tags).record(labellingProcessingTime, TimeUnit.MILLISECONDS);
            }

            if (isHostTrackingEnabled()) {
                Tags hostMetricTags = tags.and(HostMetricTag.computeMicrometerTags(rd));
                hostCounter.withTags(hostMetricTags).increment();
            }

            random(tags);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unexpected error occurred while trying to record the request into a Micrometer metric", e);
        }
    }

    private void resetMetricIfNeeded() {
        Integer resetCount = getProperty("metric.reset_count", Integer.class, 1000);
        if (requestsCounter.incrementAndGet() > resetCount) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "reset count reached ({0}). Resetting metrics", resetCount);
            }
            requestsCounter.set(0);
            registry.forEachMeter(registry::remove);
        }
    }

    private boolean isMetricRequestEnabled() {
        return getProperty("enabled", Boolean.class, false);
    }

    private boolean isHostTrackingEnabled() {
        return getProperty("metric.host.enabled", Boolean.class, false);
    }

    <T> T getProperty(String name, Class<T> target, T defaultValue) {
        T value = config.getProperty(MICROMETER, name, target);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    private Tags composeTags(RequestData rd) {
        log(rd);
        return RequestTag.computeMicrometerTags(rd);
    }

    private void random(Tags tags) {
        Random random = new Random();
        if (random.nextBoolean()) {
            tags = tags.and(
                    "status",
                    String.valueOf(RequestData.Status.values()[random.nextInt(RequestData.Status.values().length)]));

            requestTimer.withTags(tags).record(555, TimeUnit.MILLISECONDS);
            requestProcessingTimer.withTags(tags).record(555, TimeUnit.MILLISECONDS);
            responseLengthSummary.withTags(tags).record(555);
        }
    }

    private void log(RequestData rd) {
        String service = Objects.requireNonNullElse(rd.getService(), "None");
        String operation = Objects.requireNonNullElse(rd.getOperation(), "None");
        String responseStatus = Objects.requireNonNullElse(String.valueOf(rd.getResponseStatus()), "");
        String status = rd.getStatus().toString();
        String errorMessage = Objects.requireNonNullElse(rd.getErrorMessage(), "");
        String httpMethod = rd.getHttpMethod();
        String owsVersion = Objects.requireNonNullElse(rd.getOwsVersion(), "");
        String responseContentType = Objects.requireNonNullElse(rd.getResponseContentType(), "");
        String resourcesList = Objects.requireNonNullElse(rd.getResourcesList(), "");

        LOGGER.log(
                Level.WARNING,
                " labelProcessingTimes:"
                        + rd.getLabellingProcessingTime()
                        + " processingTimes:"
                        + rd.getResourcesProcessingTimeList()
                        + " resources"
                        + resourcesList
                        + " status:"
                        + status
                        + " errorMessage:"
                        + errorMessage
                        + "service:"
                        + service
                        + " httpMethod:"
                        + httpMethod
                        + " responseStatus:"
                        + responseStatus
                        + " responseContentType:"
                        + responseContentType
                        + " operation:"
                        + operation
                        + " version:"
                        + owsVersion);
    }

    @Override
    public void requestStarted(RequestData rd) {
        /* no-op */
    }

    @Override
    public void requestUpdated(RequestData rd) {
        /* no-op */
    }

    @Override
    public void requestCompleted(RequestData rd) {
        /* no-op */
    }
}
