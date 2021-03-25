/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.visitor;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.AfterVariables.AfterVariableContainer;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.standalone.DriverRunner;
import org.talend.sdk.component.api.standalone.RunAtDriver;
import org.talend.sdk.component.runtime.reflect.Parameters;

public class ModelVisitor {

    public void visit(final Class<?> type, final ModelListener listener, final boolean validate) {
        if (getSupportedComponentTypes().noneMatch(type::isAnnotationPresent)) { // unlikely but just in case
            return;
        }
        if (getSupportedComponentTypes().filter(type::isAnnotationPresent).count() != 1) { // > 1 actually
            throw new IllegalArgumentException("You can't mix @Emitter, @PartitionMapper and @Processor on " + type);
        }

        if (type.isAnnotationPresent(PartitionMapper.class)) {
            if (validate) {
                validatePartitionMapper(type);
            }
            listener.onPartitionMapper(type, type.getAnnotation(PartitionMapper.class));
        } else if (type.isAnnotationPresent(Emitter.class)) {
            if (validate) {
                validateEmitter(type);
            }
            listener.onEmitter(type, type.getAnnotation(Emitter.class));
        } else if (type.isAnnotationPresent(Processor.class)) {
            if (validate) {
                validateProcessor(type);
            }
            listener.onProcessor(type, type.getAnnotation(Processor.class));
        } else if (type.isAnnotationPresent(DriverRunner.class)) {
            if (validate) {
                validateDriverRunner(type);
            }
            listener.onDriverRunner(type, type.getAnnotation(DriverRunner.class));
        }
    }

    private void validatePartitionMapper(final Class<?> type) {
        final boolean infinite = type.getAnnotation(PartitionMapper.class).infinite();
        final long count = Stream
                .of(type.getMethods())
                .filter(m -> getPartitionMapperMethods(infinite).anyMatch(m::isAnnotationPresent))
                .flatMap(m -> getPartitionMapperMethods(infinite).filter(m::isAnnotationPresent))
                .distinct()
                .count();
        if (count != (infinite ? 2 : 3)) {
            throw new IllegalArgumentException(
                    type + " partition mapper must have exactly one @Assessor (if not infinite), "
                            + "one @Split and one @Emitter methods");
        }

        //
        // now validate the 2 methods of the mapper
        //
        if (!infinite) {
            Stream.of(type.getMethods()).filter(m -> m.isAnnotationPresent(Assessor.class)).forEach(m -> {
                if (m.getParameterCount() > 0) {
                    throw new IllegalArgumentException(m + " must not have any parameter");
                }
            });
        }
        Stream.of(type.getMethods()).filter(m -> m.isAnnotationPresent(Split.class)).forEach(m -> {
            // for now we could inject it by default but to ensure we can inject more later
            // we must do that validation
            if (Stream
                    .of(m.getParameters())
                    .anyMatch(p -> !p.isAnnotationPresent(PartitionSize.class)
                            || (p.getType() != long.class && p.getType() != int.class))) {
                throw new IllegalArgumentException(m + " must not have any parameter without @PartitionSize");
            }
            final Type splitReturnType = m.getGenericReturnType();
            if (!ParameterizedType.class.isInstance(splitReturnType)) {
                throw new IllegalArgumentException(m + " must return a Collection<" + type.getName() + ">");
            }

            final ParameterizedType splitPt = ParameterizedType.class.cast(splitReturnType);
            if (!Class.class.isInstance(splitPt.getRawType())
                    || !Collection.class.isAssignableFrom(Class.class.cast(splitPt.getRawType()))) {
                throw new IllegalArgumentException(m + " must return a List of partition mapper, found: " + splitPt);
            }

            final Type arg = splitPt.getActualTypeArguments().length != 1 ? null : splitPt.getActualTypeArguments()[0];
            if (!Class.class.isInstance(arg) || !type.isAssignableFrom(Class.class.cast(arg))) {
                throw new IllegalArgumentException(
                        m + " must return a Collection<" + type.getName() + "> but found: " + arg);
            }
        });
        Stream
                .of(type.getMethods())
                .filter(m -> m.isAnnotationPresent(Emitter.class))
                .forEach(m -> {
                    // for now we don't support injection propagation since the mapper should
                    // already own all the config
                    if (m.getParameterCount() > 0) {
                        throw new IllegalArgumentException(m + " must not have any parameter");
                    }
                });

        validateAfterVariableContainer(type);
    }

    private void validateEmitter(final Class<?> input) {
        final List<Method> producers =
                Stream.of(input.getMethods()).filter(m -> m.isAnnotationPresent(Producer.class)).collect(toList());
        if (producers.size() != 1) {
            throw new IllegalArgumentException(input + " must have a single @Producer method");
        }

        if (producers.get(0).getParameterCount() > 0) {
            throw new IllegalArgumentException(producers.get(0) + " must not have any parameter");
        }

        validateAfterVariableContainer(input);
    }

    private void validateDriverRunner(final Class<?> standalone) {
        final List<Method> driverRunners = Stream
                .of(standalone.getMethods())
                .filter(m -> m.isAnnotationPresent(RunAtDriver.class))
                .collect(toList());
        if (driverRunners.size() != 1) {
            throw new IllegalArgumentException(standalone + " must have a single @RunAtDriver method");
        }

        if (driverRunners.get(0).getParameterCount() > 0) {
            throw new IllegalArgumentException(driverRunners.get(0) + " must not have any parameter");
        }

        validateAfterVariableContainer(standalone);
    }

    private void validateProcessor(final Class<?> input) {
        final List<Method> afterGroups =
                Stream.of(input.getMethods()).filter(m -> m.isAnnotationPresent(AfterGroup.class)).collect(toList());
        afterGroups.forEach(m -> {
            final List<Parameter> invalidParams = Stream.of(m.getParameters()).peek(p -> {
                if (p.isAnnotationPresent(Output.class) && !validOutputParam(p)) {
                    throw new IllegalArgumentException("@Output parameter must be of type OutputEmitter");
                }
            })
                    .filter(p -> !p.isAnnotationPresent(Output.class))
                    .filter(p -> !Parameters.isGroupBuffer(p.getParameterizedType()))
                    .collect(toList());
            if (!invalidParams.isEmpty()) {
                throw new IllegalArgumentException("Parameter of AfterGroup method need to be annotated with Output");
            }
        });

        final List<Method> producers = Stream
                .of(input.getMethods())
                .filter(m -> m.isAnnotationPresent(ElementListener.class))
                .collect(toList());
        if (producers.size() > 1) {
            throw new IllegalArgumentException(input + " must have a single @ElementListener method");
        }
        if (producers.isEmpty() && afterGroups
                .stream()
                .noneMatch(m -> Stream.of(m.getGenericParameterTypes()).anyMatch(Parameters::isGroupBuffer))) {
            throw new IllegalArgumentException(input
                    + " must have a single @ElementListener method or pass records as a Collection<Record|JsonObject> to its @AfterGroup method");
        }

        if (!producers.isEmpty() && Stream.of(producers.get(0).getParameters()).peek(p -> {
            if (p.isAnnotationPresent(Output.class) && !validOutputParam(p)) {
                throw new IllegalArgumentException("@Output parameter must be of type OutputEmitter");
            }
        }).filter(p -> !p.isAnnotationPresent(Output.class)).count() < 1) {
            throw new IllegalArgumentException(input + " doesn't have the input parameter on its producer method");
        }

        Stream.of(input.getMethods()).filter(m -> m.isAnnotationPresent(BeforeGroup.class)).forEach(m -> {
            if (m.getParameterCount() > 0) {
                throw new IllegalArgumentException(m + " must not have any parameter");
            }
        });

        validateAfterVariableContainer(input);
    }

    private boolean validOutputParam(final Parameter p) {
        if (!ParameterizedType.class.isInstance(p.getParameterizedType())) {
            return false;
        }
        final ParameterizedType pt = ParameterizedType.class.cast(p.getParameterizedType());
        return OutputEmitter.class == pt.getRawType();
    }

    private Stream<Class<? extends Annotation>> getPartitionMapperMethods(final boolean infinite) {
        return infinite ? Stream.of(Split.class, Emitter.class) : Stream.of(Assessor.class, Split.class, Emitter.class);
    }

    private Stream<Class<? extends Annotation>> getSupportedComponentTypes() {
        return Stream.of(Emitter.class, PartitionMapper.class, Processor.class, DriverRunner.class);
    }

    private void validateAfterVariableContainer(final Class<?> type) {
        // component can't have more than one after variable container
        List<Method> markedMethods = Stream
                .of(type.getMethods())
                .filter(m -> m.isAnnotationPresent(AfterVariableContainer.class))
                .collect(toList());
        if (markedMethods.size() > 1) {
            String methods = markedMethods.stream().map(Method::toGenericString).collect(Collectors.joining(","));
            throw new IllegalArgumentException("The methods can't have more than 1 after variable container. "
                    + "Current marked methods: " + methods);
        }

        // check parameter list
        Optional
                .of(markedMethods
                        .stream()
                        .filter(m -> m.getParameterCount() != 0)
                        .map(Method::toGenericString)
                        .collect(Collectors.joining(",")))
                .filter(str -> !str.isEmpty())
                .ifPresent(str -> {
                    throw new IllegalArgumentException(
                            "The method is annotated with " + AfterVariableContainer.class.getCanonicalName() + "'"
                                    + str + "' should have parameters.");
                });

        // check incorrect return type
        Optional
                .of(markedMethods
                        .stream()
                        .filter(m -> !isValidAfterVariableContainer(m.getGenericReturnType()))
                        .map(Method::toGenericString)
                        .collect(Collectors.joining(",")))
                .filter(it -> !it.isEmpty())
                .ifPresent(methods -> {
                    throw new IllegalArgumentException(
                            "The method '" + methods + "' has wrong return type. It should be Map<String, Object>.");
                });
    }

    /**
     * Right now the valid container object for after variables is Map.
     * Where the key is String and value is Object
     */
    private static boolean isValidAfterVariableContainer(final Type type) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }

        final ParameterizedType paramType = (ParameterizedType) type;
        if (!(paramType.getRawType() instanceof Class) || paramType.getActualTypeArguments().length != 2) {
            return false;
        }

        final Class<?> containerType = (Class<?>) paramType.getRawType();
        return Map.class.isAssignableFrom(containerType) && paramType.getActualTypeArguments()[0].equals(String.class)
                && paramType.getActualTypeArguments()[1].equals(Object.class);
    }

}
