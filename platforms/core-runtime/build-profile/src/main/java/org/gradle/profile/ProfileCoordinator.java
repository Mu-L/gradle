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

package org.gradle.profile;

import org.gradle.initialization.RootBuildLifecycleListener;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.internal.time.Clock;
import org.jspecify.annotations.Nullable;

@ServiceScope(Scope.BuildTree.class)
public class ProfileCoordinator implements RootBuildLifecycleListener {
    private final BuildProfile profile;
    private final ReportGeneratingProfileListener generator;
    private final Clock clock;

    public ProfileCoordinator(BuildProfile profile, ReportGeneratingProfileListener generator, Clock clock) {
        this.profile = profile;
        this.generator = generator;
        this.clock = clock;
    }

    @Override
    public void beforeComplete(@Nullable Throwable failure) {
        profile.setBuildFinished(clock.getCurrentTime());
        generator.buildFinished(profile);
    }
}
