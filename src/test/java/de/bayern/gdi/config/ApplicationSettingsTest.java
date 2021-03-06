/*
 * DownloadClient Geodateninfrastruktur Bayern
 *
 * (c) 2016 GSt. GDI-BY (gdi.bayern.de)
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
package de.bayern.gdi.config;

import junit.framework.TestCase;
import org.junit.Test;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;

import org.w3c.dom.Document;

/**
 * Test for AutoFileNames.
 */
public class ApplicationSettingsTest extends TestCase {

    public ApplicationSettingsTest(String testName) {
        super(testName);
    }

    /**
     * test ApplicationSettings.
     * @throws Exception if anything does wrong.
     */
    @Test
    public void testApplicationSettings() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        try {
            new ApplicationSettings(doc, new Settings());
            fail("should not be reached");
        } catch (IOException ioe) {
            // Test passed.
        }
    }
}

