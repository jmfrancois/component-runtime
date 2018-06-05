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
package org.talend.sdk.component.form.internal.converter.impl.widget;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

public class ToggleWidgetConverter extends AbstractWidgetConverter {

    public ToggleWidgetConverter(final Collection<UiSchema> schemas,
            final Collection<SimplePropertyDefinition> properties, final Collection<ActionReference> actions) {
        super(schemas, properties, actions);
    }

    @Override
    public CompletionStage<PropertyContext<?>> convert(final CompletionStage<PropertyContext<?>> cs) {
        return cs.thenApply(context -> {
            final UiSchema schema = newUiSchema(context);
            schema.setWidget("toggle");
            return context;
        });
    }
}