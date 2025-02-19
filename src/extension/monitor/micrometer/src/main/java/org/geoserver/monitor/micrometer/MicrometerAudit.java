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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataListener;
import org.geotools.util.logging.Logging;

public class MicrometerAudit implements RequestDataListener {

    private static final Logger LOGGER = Logging.getLogger(MicrometerAudit.class);
    private static final String PROMETHEUS = "prometheus";

    PrometheusMeterRegistry registry;
    MonitorConfig config;

    private final Meter.MeterProvider<Timer> requestTimer;
    private final Meter.MeterProvider<DistributionSummary> responseLengthSummary;
    private final Meter.MeterProvider<Timer> requestProcessingTimer;
    private final Meter.MeterProvider<Timer> requestLabellingProcessingTimer;
    private final Meter.MeterProvider<Counter> hostCounter;

    public MicrometerAudit(MonitorConfig config, PrometheusMeterRegistry registry) {
        this.config = config;
        this.registry = registry;

        requestTimer = Timer.builder("requests").withRegistry(registry);
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
        boolean enabled = getProperty("enabled", Boolean.class, false);

        if (!enabled) {
            return;
        }

        try {
            if (rd == null) {
                return;
            }

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

            boolean trackHosts = getProperty("metric.host.enabled", Boolean.class, false);
            if (trackHosts) {

                Tags hostMetricTags = tags.and(HostMetricTag.computeMicrometerTags(rd));

                hostCounter.withTags(hostMetricTags).increment();
            }

            random(tags);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private Tags composeTags(RequestData rd) {

        log(rd);

        Tags tags = RequestTag.computeMicrometerTags(rd);

        System.err.println(tags);

        return tags;
    }

    <T> T getProperty(String name, Class<T> target, T defaultValue) {
        T value = config.getProperty(PROMETHEUS, name, target);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
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

    private static void log(RequestData rd) {
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
