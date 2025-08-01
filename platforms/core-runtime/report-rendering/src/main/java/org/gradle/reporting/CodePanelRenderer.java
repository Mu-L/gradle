/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.reporting;

import org.gradle.internal.html.SimpleHtmlWriter;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;

import static org.gradle.reporting.HtmlWriterTools.addClipboardCopyButton;

public class CodePanelRenderer extends ReportRenderer<CodePanelRenderer.Data, SimpleHtmlWriter> {
    @NullMarked
    public static final class Data {
        private final String text;
        private final String codePanelId;

        public Data(String text, String codePanelId) {
            this.text = text;
            this.codePanelId = codePanelId;
        }
    }

    @Override
    public void render(Data data, SimpleHtmlWriter htmlWriter) throws IOException {
        // Wrap in a <span>, to work around CSS problem in IE
        htmlWriter.startElement("span").attribute("class", "code")
            .startElement("pre").attribute("id", data.codePanelId).characters(data.text).endElement();
        addClipboardCopyButton(htmlWriter, data.codePanelId);
        htmlWriter.endElement();
    }
}
