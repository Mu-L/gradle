/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.provider.sources;

import java.util.Map;
import java.util.stream.Stream;

public abstract class SystemPropertiesPrefixedByValueSource extends MapWithPrefixedKeysValueSource<SystemPropertiesPrefixedByValueSource.Parameters> {
    public interface Parameters extends MapWithPrefixedKeysValueSource.Parameters {
    }

    @Override
    protected Map<String, String> collectItems(String prefix) {
        return collectWithKeyPrefix(prefix, itemsToFilter());
    }

    private static Stream<Map.Entry<String, String>> itemsToFilter() {
        return System.getProperties().entrySet().stream()
            .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
            .map(e -> {
                Map.Entry<?, ?> untypedEntry = e;
                @SuppressWarnings("unchecked")
                Map.Entry<String, String> stringEntry = (Map.Entry<String, String>) untypedEntry;
                return stringEntry;
            });
    }
}
