/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.microprofile.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.metrics.MetricsSupport;
import io.helidon.metrics.RegistryFactory;
import io.helidon.microprofile.server.RoutingBuilders;
import io.helidon.microprofile.server.spi.MpService;
import io.helidon.microprofile.server.spi.MpServiceContext;

/**
 * Extension of microprofile {@link io.helidon.microprofile.server.Server} to enable support for metrics
 * and metrics REST API.
 * This handles only API of metrics. CDI support is in a separate module.
 *
 * <p>
 * To use metrics system, you can either use static methods on (proprietary) {@link RegistryFactory}
 * or use CDI annotations.
 */
public class MetricsMpService implements MpService {
    @Override
    public void configure(MpServiceContext serviceContext) {
        Set<String> vendorMetricsAdded = new HashSet<>();

        Config metricsConfig = serviceContext.helidonConfig().get("metrics");

        MetricsSupport metricsSupport = MetricsSupport.create(metricsConfig);

        RoutingBuilders routingBuilders = RoutingBuilders.create(
                serviceContext, metricsConfig);

        metricsSupport.configureVendorMetrics(null, routingBuilders.defaultRoutingBuilder());
        vendorMetricsAdded.add("@default");
        metricsSupport.configureEndpoint(routingBuilders.routingBuilder());

        // now we may have additional sockets we want to add vendor metrics to
        metricsConfig.get("vendor-metrics-routings")
                .asList(String.class)
                .orElseGet(List::of)
                .forEach(routeName -> {
                    if (!vendorMetricsAdded.contains(routeName)) {
                        metricsSupport.configureVendorMetrics(routeName, serviceContext.serverNamedRoutingBuilder(routeName));
                        vendorMetricsAdded.add(routeName);
                    }
                });
    }
}
