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
package org.talend.sdk.component.server.front;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.PATH;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.COMPONENT_MISSING;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.DESIGN_MODEL_MISSING;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.PLUGIN_MISSING;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.design.extension.DesignModel;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.extension.ComponentContexts;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.dao.ComponentDao;
import org.talend.sdk.component.server.dao.ComponentFamilyDao;
import org.talend.sdk.component.server.front.base.internal.RequestKey;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentId;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.Dependencies;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.Icon;
import org.talend.sdk.component.server.front.model.Link;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.ActionsService;
import org.talend.sdk.component.server.service.ComponentManagerService;
import org.talend.sdk.component.server.service.IconResolver;
import org.talend.sdk.component.server.service.LocaleMapper;
import org.talend.sdk.component.server.service.PropertiesService;
import org.talend.sdk.component.spi.component.ComponentExtension;

import lombok.extern.slf4j.Slf4j;

@Tag(name = "Component", description = "Endpoints related to component metadata access.")
@Slf4j
@Path("component")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComponentResource {

    private final ConcurrentMap<RequestKey, ComponentIndices> indicesPerRequest = new ConcurrentHashMap<>();

    @Inject
    private ComponentManager manager;

    @Inject
    private ComponentManagerService componentManagerService;

    @Inject
    private ComponentDao componentDao;

    @Inject
    private ComponentFamilyDao componentFamilyDao;

    @Inject
    private LocaleMapper localeMapper;

    @Inject
    private ActionsService actionsService;

    @Inject
    private PropertiesService propertiesService;

    @Inject
    private IconResolver iconResolver;

    @Inject
    private ComponentServerConfiguration configuration;

    @PostConstruct
    private void setupRuntime() {
        log.info("Initializing " + getClass());

        // preload some highly used data
        getIndex("en", false);
    }

    @GET
    @Path("dependencies")
    @Operation(description = "Returns a list of dependencies for the given components. "
            + "IMPORTANT: don't forget to add the component itself since it will not be part of the dependencies."
            + "Then you can use /dependency/{id} to download the binary.")
    @APIResponse(responseCode = "200", description = "The list of dependencies per component",
            content = @Content(mediaType = APPLICATION_JSON))
    public Dependencies
            getDependencies(@QueryParam("identifier") @Parameter(name = "identifier",
                    description = "the list of component identifiers to find the dependencies for.",
                    in = QUERY) final String[] ids) {
        if (ids.length == 0) {
            return new Dependencies(emptyMap());
        }
        return new Dependencies(Stream
                .of(ids)
                .map(id -> componentDao.findById(id))
                .collect(toMap(ComponentFamilyMeta.BaseMeta::getId,
                        meta -> componentManagerService.manager().findPlugin(meta.getParent().getPlugin()).map(c -> {
                            ComponentExtension.ComponentContext context =
                                    c.get(ComponentContexts.class).getContexts().get(meta.getType());
                            ComponentExtension extension = context.owningExtension();
                            final Stream<Artifact> deps = c.findDependencies();
                            final Stream<Artifact> artifacts;
                            if (configuration.getAddExtensionDependencies() && extension != null) {
                                final List<Artifact> dependencies = deps.collect(toList());
                                final Stream<Artifact> addDeps = extension
                                        .getAdditionalDependencies()
                                        .stream()
                                        .map(Artifact::from)
                                        // filter required artifacts if they are already present in the list.
                                        .filter(extArtifact -> dependencies
                                                .stream()
                                                .map(d -> d.getGroup() + ":" + d.getArtifact())
                                                .noneMatch(ga -> ga
                                                        .equals(extArtifact.getGroup() + ":"
                                                                + extArtifact.getArtifact())));
                                artifacts = Stream.concat(dependencies.stream(), addDeps);
                            } else {
                                artifacts = deps;
                            }
                            return new DependencyDefinition(artifacts.map(Artifact::toCoordinate).collect(toList()));
                        }).orElse(new DependencyDefinition(emptyList())))));
    }

    @GET
    @Path("dependency/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Return a binary of the dependency represented by `id`. "
            + "It can be maven coordinates for dependencies or a component id.")
    @APIResponse(responseCode = "200", description = "The dependency binary (jar).",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    public StreamingOutput getDependency(@PathParam("id") @Parameter(name = "id",
            description = "the dependency binary (jar).", in = PATH) final String id) {
        final ComponentFamilyMeta.BaseMeta<?> component = componentDao.findById(id);
        final File file;
        if (component != null) { // local dep
            file = componentManagerService
                    .manager()
                    .findPlugin(component.getParent().getPlugin())
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.NOT_FOUND)
                            .type(APPLICATION_JSON_TYPE)
                            .entity(new ErrorPayload(PLUGIN_MISSING, "No plugin matching the id: " + id))
                            .build()))
                    .getContainerFile()
                    .orElseThrow(() -> new WebApplicationException(Response
                            .status(Response.Status.NOT_FOUND)
                            .type(APPLICATION_JSON_TYPE)
                            .entity(new ErrorPayload(PLUGIN_MISSING, "No dependency matching the id: " + id))
                            .build()));
        } else { // just try to resolve it locally, note we would need to ensure some security here
            // .map(Artifact::toPath).map(localDependencyRelativeResolver
            final Artifact artifact = Artifact.from(id);
            file = componentManagerService.manager().getContainer().resolve(artifact.toPath());
        }
        if (!file.exists()) {
            throw new WebApplicationException(Response
                    .status(Response.Status.NOT_FOUND)
                    .type(APPLICATION_JSON_TYPE)
                    .entity(new ErrorPayload(PLUGIN_MISSING, "No file found for: " + id))
                    .build());
        }
        return output -> {
            final byte[] buffer = new byte[40960]; // 5k
            try (final InputStream stream = new BufferedInputStream(new FileInputStream(file), buffer.length)) {
                int count;
                while ((count = stream.read(buffer)) >= 0) {
                    if (count == 0) {
                        continue;
                    }
                    output.write(buffer, 0, count);
                }
            }
        };
    }

    @GET
    @Path("index")
    @Operation(description = "Returns the list of available components.")
    @APIResponse(responseCode = "200", description = "The index of available components.",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    public ComponentIndices getIndex(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) final String language,
            @QueryParam("includeIconContent") @DefaultValue("false") @Parameter(name = "includeIconContent",
                    description = "should the icon binary format be included in the payload.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) final boolean includeIconContent) {
        final Locale locale = localeMapper.mapLocale(language);
        return indicesPerRequest
                .computeIfAbsent(new RequestKey(locale, includeIconContent), k -> new ComponentIndices(manager
                        .find(c -> c
                                .execute(
                                        () -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                                .flatMap(component -> Stream
                                        .concat(component
                                                .getPartitionMappers()
                                                .values()
                                                .stream()
                                                .map(mapper -> toComponentIndex(c, locale, c.getId(), mapper,
                                                        c.get(ComponentManager.OriginalId.class), includeIconContent)),
                                                component
                                                        .getProcessors()
                                                        .values()
                                                        .stream()
                                                        .map(proc -> toComponentIndex(c, locale, c.getId(), proc,
                                                                c.get(ComponentManager.OriginalId.class),
                                                                includeIconContent)))))
                        .collect(toList())));
    }

    @GET
    @Path("icon/family/{id}")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM })
    @Operation(description = "Returns the icon for a family.")
    @APIResponse(responseCode = "200", description = "Returns a particular family icon in raw bytes.",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "404", description = "The family or icon is not found",
            content = @Content(mediaType = APPLICATION_JSON))
    public Response familyIcon(@PathParam("id") @Parameter(name = "id", description = "the family identifier",
            in = PATH) final String id) {
        // todo: add caching if SvgIconResolver becomes used a lot - not the case ATM
        final ComponentFamilyMeta meta = componentFamilyDao.findById(id);
        if (meta == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.FAMILY_MISSING, "No family for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
        final Optional<Container> plugin = manager.findPlugin(meta.getPlugin());
        if (!plugin.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.PLUGIN_MISSING,
                            "No plugin '" + meta.getPlugin() + "' for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final IconResolver.Icon iconContent = iconResolver.resolve(plugin.get(), meta.getIcon());
        if (iconContent == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for family identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(iconContent.getBytes()).type(iconContent.getType()).build();
    }

    @GET
    @Path("icon/{id}")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM })
    @Operation(description = "Returns a particular component icon in raw bytes.")
    @APIResponse(responseCode = "200", description = "The component icon in binary form.",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "404", description = "The family or icon is not found",
            content = @Content(mediaType = APPLICATION_JSON))
    public Response icon(@PathParam("id") @Parameter(name = "id", description = "the component icon identifier",
            in = PATH) final String id) {
        // todo: add caching if SvgIconResolver becomes used a lot - not the case ATM
        final ComponentFamilyMeta.BaseMeta<Object> meta = componentDao.findById(id);
        if (meta == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "No component for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final Optional<Container> plugin = manager.findPlugin(meta.getParent().getPlugin());
        if (!plugin.isPresent()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.PLUGIN_MISSING,
                            "No plugin '" + meta.getParent().getPlugin() + "' for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        final IconResolver.Icon iconContent = iconResolver.resolve(plugin.get(), meta.getIcon());
        if (iconContent == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorPayload(ErrorDictionary.ICON_MISSING, "No icon for identifier: " + id))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(iconContent.getBytes()).type(iconContent.getType()).build();
    }

    @POST
    @Path("migrate/{id}/{configurationVersion}")
    @Operation(description = "Allows to migrate a component configuration without calling any component execution.")
    @APIResponse(responseCode = "200",
            description = "the new configuration for that component (or the same if no migration was needed).",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "The component is not found",
            content = @Content(mediaType = APPLICATION_JSON))
    public Map<String, String>
            migrate(@PathParam("id") @Parameter(name = "id", description = "the component identifier",
                    in = PATH) final String id,
                    @PathParam("configurationVersion") @Parameter(name = "configurationVersion",
                            description = "the configuration version you send", in = PATH) final int version,
                    @RequestBody(description = "the actual configuration in key/value form.", required = true,
                            content = @Content(mediaType = APPLICATION_JSON,
                                    schema = @Schema(type = OBJECT))) final Map<String, String> config) {
        return ofNullable(componentDao.findById(id))
                .orElseThrow(() -> new WebApplicationException(Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(new ErrorPayload(ErrorDictionary.COMPONENT_MISSING, "Didn't find component " + id))
                        .build()))
                .getMigrationHandler()
                .migrate(version, config);
    }

    @GET // TODO: max ids.length
    @Path("details") // bulk mode to avoid to fetch components one by one when reloading a pipeline/job
    @Operation(description = "Returns the set of metadata about a few components identified by their 'id'.")
    @APIResponse(responseCode = "200", description = "the list of details for the requested components.",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Some identifiers were not valid.",
            content = @Content(mediaType = APPLICATION_JSON))
    public ComponentDetailList getDetail(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) final String language,
            @QueryParam("identifiers") @Parameter(name = "identifiers",
                    description = "the component identifiers to request.", in = QUERY) final String[] ids) {

        if (ids == null || ids.length == 0) {
            return new ComponentDetailList(emptyList());
        }

        final Map<String, ErrorPayload> errors = new HashMap<>();
        final List<ComponentDetail> details =
                Stream.of(ids).map(id -> ofNullable(componentDao.findById(id)).orElseGet(() -> {
                    errors.put(id, new ErrorPayload(COMPONENT_MISSING, "No component '" + id + "'"));
                    return null;
                })).filter(Objects::nonNull).map(meta -> {
                    final Optional<Container> plugin = manager.findPlugin(meta.getParent().getPlugin());
                    if (!plugin.isPresent()) {
                        errors
                                .put(meta.getId(), new ErrorPayload(PLUGIN_MISSING,
                                        "No plugin '" + meta.getParent().getPlugin() + "'"));
                        return null;
                    }

                    final Container container = plugin.get();

                    final Optional<DesignModel> model = ofNullable(meta.get(DesignModel.class));
                    if (!model.isPresent()) {
                        errors
                                .put(meta.getId(), new ErrorPayload(DESIGN_MODEL_MISSING,
                                        "No design model '" + meta.getId() + "'"));
                        return null;
                    }

                    final Locale locale = localeMapper.mapLocale(language);

                    final ComponentDetail componentDetail = new ComponentDetail();
                    componentDetail.setLinks(emptyList() /* todo ? */);
                    componentDetail.setId(createMetaId(container, meta));
                    componentDetail.setVersion(meta.getVersion());
                    componentDetail.setIcon(meta.getIcon());
                    componentDetail.setInputFlows(model.get().getInputFlows());
                    componentDetail.setOutputFlows(model.get().getOutputFlows());
                    componentDetail
                            .setType(ComponentFamilyMeta.ProcessorMeta.class.isInstance(meta) ? "processor" : "input");
                    componentDetail
                            .setDisplayName(meta
                                    .findBundle(container.getLoader(), locale)
                                    .displayName()
                                    .orElse(meta.getName()));
                    componentDetail
                            .setProperties(propertiesService
                                    .buildProperties(meta.getParameterMetas(), container.getLoader(), locale, null)
                                    .collect(toList()));
                    componentDetail
                            .setActions(actionsService
                                    .findActions(meta.getParent().getName(), container, locale, meta,
                                            meta.getParent().findBundle(container.getLoader(), locale)));

                    return componentDetail;
                }).filter(Objects::nonNull).collect(toList());

        if (!errors.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(errors).build());
        }

        return new ComponentDetailList(details);
    }

    private ComponentId createMetaId(final Container container, final ComponentFamilyMeta.BaseMeta<Object> meta) {
        return new ComponentId(meta.getId(), meta.getParent().getId(), meta.getParent().getPlugin(),
                ofNullable(container.get(ComponentManager.OriginalId.class))
                        .map(ComponentManager.OriginalId::getValue)
                        .orElse(container.getId()),
                meta.getParent().getName(), meta.getName());
    }

    private ComponentIndex toComponentIndex(final Container container, final Locale locale, final String plugin,
            final ComponentFamilyMeta.BaseMeta meta, final ComponentManager.OriginalId originalId,
            final boolean includeIcon) {
        final ClassLoader loader = container.getLoader();
        final String icon = meta.getIcon();
        final String familyIcon = meta.getParent().getIcon();
        final IconResolver.Icon iconContent = iconResolver.resolve(container, icon);
        final IconResolver.Icon iconFamilyContent = iconResolver.resolve(container, familyIcon);
        final String familyDisplayName =
                meta.getParent().findBundle(loader, locale).displayName().orElse(meta.getParent().getName());
        final List<String> categories = ofNullable(meta.getParent().getCategories())
                .map(vals -> vals
                        .stream()
                        .map(this::normalizeCategory)
                        .map(category -> category.replace("${family}", meta.getParent().getName())) // not
                                                                                                    // i18n-ed
                                                                                                    // yet
                        .map(category -> meta
                                .getParent()
                                .findBundle(loader, locale)
                                .category(category)
                                .orElseGet(() -> category
                                        .replace("/" + meta.getParent().getName() + "/",
                                                "/" + familyDisplayName + "/")))
                        .collect(toList()))
                .orElseGet(Collections::emptyList);
        return new ComponentIndex(
                new ComponentId(meta.getId(), meta.getParent().getId(), plugin,
                        ofNullable(originalId).map(ComponentManager.OriginalId::getValue).orElse(plugin),
                        meta.getParent().getName(), meta.getName()),
                meta.findBundle(loader, locale).displayName().orElse(meta.getName()), familyDisplayName,
                new Icon(icon, iconContent == null ? null : iconContent.getType(),
                        !includeIcon ? null : (iconContent == null ? null : iconContent.getBytes())),
                new Icon(familyIcon, iconFamilyContent == null ? null : iconFamilyContent.getType(),
                        !includeIcon ? null : (iconFamilyContent == null ? null : iconFamilyContent.getBytes())),
                meta.getVersion(), categories, singletonList(new Link("Detail",
                        "/component/details?identifiers=" + meta.getId(), MediaType.APPLICATION_JSON)));
    }

    private String normalizeCategory(final String category) {
        // we prevent root categories and always append the family in this case
        if (!category.contains("${family}")) {
            return category + "/${family}";
        }
        return category;
    }
}
