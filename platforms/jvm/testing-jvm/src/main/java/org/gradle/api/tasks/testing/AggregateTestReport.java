/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.api.tasks.testing;

import org.gradle.api.Incubating;
import org.gradle.api.provider.Property;
import org.gradle.api.reporting.ReportSpec;
import org.gradle.api.tasks.TaskProvider;

/**
 * A container for the inputs of an aggregated test report.
 *
 * @since 7.4
 */
@Incubating
public interface AggregateTestReport extends ReportSpec {

    /**
     * Contains the {@link TestReport} task instance which produces this report.
     *
     * @return the task instance
     */
    TaskProvider<TestReport> getReportTask();

    /**
     * Contains the name of the test suite in target projects that this report will aggregate.
     *
     * @return the name of the suite that this report will aggregate.
     *
     * @since 8.13
     */
    Property<String> getTestSuiteName();
}
