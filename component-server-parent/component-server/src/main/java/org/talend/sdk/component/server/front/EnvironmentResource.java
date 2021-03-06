/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.server.front.model.Environment;
import org.talend.sdk.component.server.service.ComponentManagerService;

@Tag(name = "Environment", description = "Endpoint giving access to versions and last update timestamp of the server.")
@Path("environment")
@ApplicationScoped
public class EnvironmentResource {

    private final AtomicReference<Environment> environment = new AtomicReference<>();

    @Inject
    @ConfigProperty(name = "git.build.version")
    private String version;

    @Inject
    @ConfigProperty(name = "git.commit.id")
    private String commit;

    @Inject
    @ConfigProperty(name = "git.build.time")
    private String time;

    @Inject
    private Instance<Application> applications;

    @Inject
    private ComponentManagerService service;

    private int latestApiVersion;

    @PostConstruct
    private void init() {
        latestApiVersion = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(applications.iterator(), Spliterator.IMMUTABLE), false)
                .filter(a -> a.getClass().isAnnotationPresent(ApplicationPath.class))
                .map(a -> a.getClass().getAnnotation(ApplicationPath.class).value())
                .map(path -> path.replace("api/v", ""))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(1);
    }

    @GET
    @Operation(
            description = "Returns the environment of this instance. Useful to check the version or configure a healthcheck for the server.")
    @APIResponse(responseCode = "200", description = "Current environment representation.",
            content = @Content(mediaType = APPLICATION_JSON))
    public Environment get() {
        return new Environment(latestApiVersion, version, commit, time, service.findLastUpdated());
    }
}
