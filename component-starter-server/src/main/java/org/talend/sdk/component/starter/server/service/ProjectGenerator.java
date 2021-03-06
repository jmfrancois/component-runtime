/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.talend.sdk.component.starter.server.service.build.BuildGenerator;
import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.event.CreateProject;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.facet.component.ComponentGenerator;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import lombok.Getter;

@ApplicationScoped
public class ProjectGenerator {

    @Getter
    private final Map<String, BuildGenerator> generators = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @Getter
    private final Map<String, FacetGenerator> facets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @Inject
    private Event<GeneratorRegistration> registrationEvent;

    @Inject
    private ReadmeGenerator readmeGenerator;

    @Inject
    private Event<CreateProject> onCreate;

    @Inject
    private ComponentGenerator componentGenerator;

    @Inject
    private TemplateRenderer tpl;

    @Inject
    private ServerInfo versions;

    private List<String> scopesOrdering;

    @PostConstruct
    private void init() {
        final GeneratorRegistration event = new GeneratorRegistration();
        registrationEvent.fire(event);
        generators.putAll(event.getBuildGenerators());
        facets.putAll(event.getFacetGenerators());
        scopesOrdering = asList("provided", "compile", "runtime", "test");
    }

    public void generate(final ProjectRequest request, final OutputStream outputStream) {
        final ServerInfo.Snapshot versionSnapshot = versions.getSnapshot();
        final BuildGenerator generator = generators.get(request.getBuildType());
        final Map<String, byte[]> files = new HashMap<>();

        // build dependencies to give them to the build
        final Collection<String> facets = ofNullable(request.getFacets()).orElse(emptyList());
        final List<Dependency> dependencies = facets
                .stream()
                .map(this.facets::get)
                .flatMap(f -> f.dependencies(facets, versionSnapshot))
                .distinct()
                .sorted((o1, o2) -> {
                    { // by scope
                        final int scope1 = scopesOrdering.indexOf(o1.getScope());
                        final int scope2 = scopesOrdering.indexOf(o2.getScope());
                        final int scopeDiff = scope1 - scope2;
                        if (scopeDiff != 0) {
                            return scopeDiff;
                        }
                    }

                    { // by group
                        final int comp = o1.getGroup().compareTo(o2.getGroup());
                        if (comp != 0) {
                            return comp;
                        }
                    }

                    // by name
                    return o1.getArtifact().compareTo(o2.getArtifact());
                })
                .collect(toList());
        // force component-api and force it first
        final Dependency componentApi = Dependency.componentApi(versionSnapshot.getApiKit());
        dependencies.remove(componentApi);
        dependencies.add(0, componentApi);

        // create the build to be able to generate the files
        final Build build = generator
                .createBuild(request.getBuildConfiguration(), request.getPackageBase(), dependencies, facets,
                        versionSnapshot);
        files.put(build.getBuildFileName(), build.getBuildFileContent().getBytes(StandardCharsets.UTF_8));

        // generate facet files
        final Map<FacetGenerator, List<String>> filePerFacet =
                facets.stream().map(s -> s.toLowerCase(Locale.ENGLISH)).collect(toMap(this.facets::get, f -> {
                    final FacetGenerator g = this.facets.get(f);
                    return g
                            .create(request.getPackageBase(), build, facets, request.getSources(),
                                    request.getProcessors(), versionSnapshot)
                            .peek(file -> files.put(file.getPath(), file.getContent()))
                            .map(FacetGenerator.InMemoryFile::getPath)
                            .collect(toList());
                }));

        // generate README.adoc if needed
        if (!files.containsKey("README.adoc")) {
            files
                    .put("README.adoc",
                            readmeGenerator
                                    .createReadme(request.getBuildConfiguration().getName(), filePerFacet)
                                    .getBytes(StandardCharsets.UTF_8));
        }

        // handle logging - centralized since we want a single setup per project
        filePerFacet.keySet().stream().map(FacetGenerator::loggingScope).reduce((s1, s2) -> {
            final List<String> scopes = asList(s1, s2);
            if (scopes.contains("compile")) {
                return "compile";
            }
            if (scopes.contains("provided")) {
                return "provided";
            }
            if (scopes.contains("test")) {
                return "test";
            }
            return s1;
        }).filter(s -> !s.isEmpty()).ifPresent(scope -> {
            dependencies
                    .add(new Dependency("org.apache.logging.log4j", "log4j-slf4j-impl", versionSnapshot.getLog4j2(),
                            scope));
            files
                    .put(("test".equals(scope) ? build.getTestResourcesDirectory() : build.getMainResourcesDirectory())
                            + "/log4j2.xml",
                            tpl
                                    .render("generator/logging/log4j2.mustache", emptyMap())
                                    .getBytes(StandardCharsets.UTF_8));
        });

        componentGenerator
                .create(request.getPackageBase(), build, request.getFamily(), request.getCategory(),
                        request.getSources(), request.getProcessors())
                .forEach(file -> files.put(file.getPath(), file.getContent()));

        // add wrapper build files
        build.getWrapperFiles().forEach(f -> files.put(f.getPath(), f.getContent()));

        // now create the zip prefixing it with the artifact value
        final String rootName = request.getBuildConfiguration().getArtifact();
        final Set<String> createdFolders = new HashSet<>();
        try (final ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            // first create folders
            new HashSet<>(files.keySet()).forEach(path -> {
                final String[] segments = (rootName + '/' + path).split("/");
                final StringBuilder current = new StringBuilder();
                for (int i = 0; i < segments.length; i++) {
                    if (i == segments.length - 1) {
                        break;
                    }

                    current.append(segments[i]).append('/');

                    final String folder = current.toString();
                    if (createdFolders.add(folder)) {
                        try {
                            zip.putNextEntry(new ZipEntry(folder));
                            zip.closeEntry();
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
            });

            // now create files entries
            files.forEach((path, content) -> {
                try {
                    zip.putNextEntry(new ZipEntry(rootName + '/' + path));
                    zip.write(content);
                    zip.closeEntry();
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        onCreate.fire(new CreateProject(request));
    }

}
