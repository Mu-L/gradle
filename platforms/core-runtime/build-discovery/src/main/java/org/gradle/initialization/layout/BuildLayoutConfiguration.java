/*
 * Copyright 2012 the original author or authors.
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
package org.gradle.initialization.layout;

import org.gradle.initialization.BuildLayoutParameters;

import java.io.File;

/**
 * Configuration which affects the (static) layout of a build.
 */
public class BuildLayoutConfiguration {
    private final File currentDir;
    private final boolean searchUpwards;
    private final boolean useEmptySettings;

    public BuildLayoutConfiguration(File currentDir, boolean searchUpwards, boolean useEmptySettings) {
        this.currentDir = currentDir;
        this.searchUpwards = searchUpwards;
        this.useEmptySettings = useEmptySettings;
    }

    public BuildLayoutConfiguration(BuildLayoutParameters parameters) {
        this.currentDir = parameters.getCurrentDir();
        this.searchUpwards = true;
        this.useEmptySettings = false;
    }

    public File getCurrentDir() {
        return currentDir;
    }

    public boolean isSearchUpwards() {
        return searchUpwards;
    }

    public boolean isUseEmptySettings() {
        return useEmptySettings;
    }
}
