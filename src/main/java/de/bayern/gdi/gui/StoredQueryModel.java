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

import de.bayern.gdi.services.WFSMeta;

/**
 * Wrapper for stored queries.
 */
public class StoredQueryModel implements ItemModel {

    private WFSMeta.StoredQuery query;

    /**
     * Construct the wrapper.
     * @param q The wrapped object.
     */
    public StoredQueryModel(WFSMeta.StoredQuery q) {
        this.query = q;
    }

    public Object getItem() {
        return this.query;
    }

    public String getDataset() {
        return this.query.getID();
    }


    @Override
    public String toString() {
        return this.query.getTitle();
    }
}
