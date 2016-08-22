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
package de.bayern.gdi.gui;

import de.bayern.gdi.utils.I18n;

/**
 * UI model for services.
 */
public class ServiceModel {

    private String name;
    private String url;
    private String version;
    private boolean restricted;

    /**
     * Get the service name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the service name.
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the service url.
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the service url.
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get the service version.
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the service version.
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Is authentication required?
     * @return the restricted
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Set authentication required.
     * @param restricted the restricted to set
     */
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    @Override
    public String toString() {
        if (this.restricted) {
            return  I18n.format("gui.restricted", this.name);
        }
        return this.name;
    }
}