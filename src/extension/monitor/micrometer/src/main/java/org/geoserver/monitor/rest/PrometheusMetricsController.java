package org.geoserver.monitor.rest;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.geoserver.rest.RestBaseController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrometheusMetricsController {
    PrometheusMeterRegistry registry;

    public PrometheusMetricsController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    @RequestMapping(
            value = RestBaseController.ROOT_PATH + "/monitor/requests/metrics",
            produces = MediaType.TEXT_PLAIN_VALUE)
    protected String scrape() {

        return registry.scrape();
    }
}
