package org.geoserver.monitor.micrometer;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geoserver.monitor.RequestData;

interface MetricsTag {

    String DEFAULT_VALUE = "None";

    default String noneDefault(String value) {
        return Objects.requireNonNullElse(value, DEFAULT_VALUE);
    }

    default String emptyDefault(String value) {
        return Objects.requireNonNullElse(value, "");
    }

    static Tags toMicrometerTags(RequestData requestData, MetricsTag... metricsTags) {
        return Tags.of(Arrays.stream(metricsTags)
                .map(t -> Tag.of(t.tagName(), t.compute(requestData)))
                .collect(Collectors.toList()));
    }

    String tagName();

    String compute(RequestData requestData);
}
