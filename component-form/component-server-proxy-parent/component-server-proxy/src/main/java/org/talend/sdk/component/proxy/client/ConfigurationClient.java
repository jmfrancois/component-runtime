/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.proxy.client;

import static java.util.Collections.emptyMap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.talend.sdk.component.proxy.config.ProxyConfiguration;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;

@ApplicationScoped
public class ConfigurationClient {

    @Inject
    private WebTarget webTarget;

    @Inject
    private ProxyConfiguration configuration;

    public CompletionStage<ConfigTypeNodes> getAllConfigurations(final String language,
            final Function<String, String> placeholderProvider) {
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("configurationtype/index")
                        .queryParam("language", language)
                        .queryParam("lightPayload", true)
                        .request(MediaType.APPLICATION_JSON_TYPE), placeholderProvider)
                .rx()
                .get(ConfigTypeNodes.class);
    }

    public CompletionStage<ConfigTypeNodes> getDetails(final String language, final String[] ids,
            final Function<String, String> placeholderProvider) {
        if (ids == null || ids.length == 0) {
            return CompletableFuture.completedFuture(new ConfigTypeNodes(emptyMap()));
        }
        return configuration
                .getHeaderAppender()
                .apply(this.webTarget
                        .path("configurationtype/details")
                        .queryParam("language", language)
                        .queryParam("identifiers", ids)
                        .request(MediaType.APPLICATION_JSON_TYPE), placeholderProvider)
                .rx()
                .get(ConfigTypeNodes.class);
    }
}
