package org.geoserver.monitor.micrometer;

import io.micrometer.core.instrument.Tags;
import org.geoserver.monitor.RequestData;

enum HostMetricTag implements MetricsTag {
    REMOTE_ADDR("remoteAddr") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getRemoteAddr());
        }
    },
    REMOTE_HOST("remoteHost") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getRemoteHost());
        }
    },
    REMOTE_USER("remoteUser") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getRemoteUser());
        }
    };

    private static final HostMetricTag[] VALUES = values();
    private final String name;

    HostMetricTag(String name) {
        this.name = name;
    }

    public static Tags computeMicrometerTags(RequestData requestData) {
        return MetricsTag.toMicrometerTags(requestData, VALUES);
    }

    @Override
    public String tagName() {
        return this.name;
    }
}
