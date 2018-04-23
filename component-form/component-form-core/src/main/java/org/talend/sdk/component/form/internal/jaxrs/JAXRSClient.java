/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.form.internal.jaxrs;

import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.talend.sdk.component.form.api.Client;

public class JAXRSClient implements Client {

    private final javax.ws.rs.client.Client delegate;

    private final WebTarget target;

    private final boolean closeClient;

    private final GenericType<Map<String, Object>> mapType;

    public JAXRSClient(final String base) {
        this(newClient(), base, true);
    }

    public JAXRSClient(final javax.ws.rs.client.Client client, final String base, final boolean closeClient) {
        this.delegate = client;
        this.closeClient = closeClient;
        this.target = client.target(base);
        this.mapType = new GenericType<Map<String, Object>>() {
        };
    }

    @Override
    public CompletableFuture<Map<String, Object>> action(final String family, final String type, final String action,
            final Map<String, Object> params) {
        final Map<Object, Object> payload =
                params.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        return target
                .path("action/execute")
                .queryParam("family", family)
                .queryParam("type", type)
                .queryParam("action", action)
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .post(entity(payload, APPLICATION_JSON_TYPE), mapType)
                .toCompletableFuture();
    }

    @Override
    public void close() {
        if (closeClient) {
            delegate.close();
        }
    }

    private static javax.ws.rs.client.Client newClient() {
        final javax.ws.rs.client.Client instance = ClientBuilder.newClient();
        System
                .getProperties()
                .stringPropertyNames()
                .stream()
                .filter(k -> k.startsWith("talend.component.form.client.jaxrs.properties."))
                .forEach(k -> instance.property(k.substring("talend.component.form.client.jaxrs.properties.".length()),
                        System.getProperty(k)));
        return instance;
    }
}