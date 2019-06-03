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

package de.bayern.gdi.gui.map;


import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.CoordinateLine;
import com.sothawo.mapjfx.Extent;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.WMSParam;
import com.sothawo.mapjfx.event.MapViewEvent;
import de.bayern.gdi.utils.Config;
import de.bayern.gdi.utils.HTTP;
import de.bayern.gdi.utils.I18n;
import de.bayern.gdi.utils.ServiceSettings;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.map.WMSLayer;
import org.geotools.referencing.CRS;
import org.geotools.styling.StyleBuilder;
import org.geotools.swing.locale.LocaleUtils;
import org.geotools.swing.tool.InfoToolHelper;
import org.geotools.swing.tool.InfoToolResult;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author Jochen Saalfeld (jochen@intevation.de)
 */

public class WMSMapSwing extends Parent {

    private static final Logger LOG
        = LoggerFactory.getLogger(WMSMapSwing.class.getName());

    /**
     * name of the polygon layer.
     */
    public static final String POLYGON_LAYER_TITLE = "PolygonLayer";
    private static final int SOURCE_LABEL_HEIGHT = 70;
    private static final int MAP_WIDTH = 400;
    private static final int MAP_HEIGHT = 250;
    private static final double TEN_PERCENT = 0.1D;
    private static final String TOOLBAR_INFO_BUTTON_NAME = "ToolbarInfoButton";
    private static final String TOOLBAR_POINTER_BUTTON_NAME
        = "ToolbarPointerButton";
    private static final String TOOLBAR_RESET_BUTTON_NAME
        = "ToolbarResetButton";
    private static final double BAY_MAX_LAT = 50.654523743525;
    private static final double BAY_MIN_LAT = 47.2178956772476;
    private static final double BAY_MIN_LON = 7.63593144329548;
    private static final double BAY_MAX_LON = 15.1069052509681;
    private static final String INITIAL_CRS = "EPSG:4326";

    private WebMapServer wms;
    private MapView mapView;
    private StyleBuilder sb;
    private DefaultFeatureCollection polygonFeatureCollection;
    private CoordinateReferenceSystem mapCRS;
    private ToggleButton infoButton;
    private ToggleButton bboxButton;
    private Coordinate bboxFirst;
    private CoordinateLine currentBbox;
    private WMSLayer layer;
    private MapContent mapContent;
    private BboxCoordinates bboxCoordinates;
    private PolygonsOnMapViewHandler polygonsOnMapViewHandler;
    private FeatureInfoReporter featureInfoReporter = new FeatureInfoReporter();


    /**
     * Initializes geotools localisation system with our default I18n locale.
     * TODO: Buggy with org/geotools/gt-swing/14.3 (and probably also w 15.0.)
     */
    private static void initGeotoolsLocale() {
        LocaleUtils.setLocale(I18n.getLocale());
    }

    /**
     * gets the getCapabilities URL.
     *
     * @param mapURL the URL of the Map
     * @return getCapabilties URL
     */
    /*
    public static URL getCapabiltiesURL(URL mapURL) {
    URL url = mapURL;
    try {
    WebMapServer wms = new WebMapServer(mapURL);
    HTTPClient httpClient = wms.getHTTPClient();
    URL get = wms.
    getCapabilities().
    getRequest().
    getGetCapabilities().
    getGet();
    if (get != null) {
    url = new URL(get.toString() + "request=GetCapabilities");
    }
    httpClient.getConnectTimeout();
    } catch (IOException | ServiceException e) {
    LOG.error(e.getMessage(), e);
    }
    return url;
    }
    */

    /**
     * Constructor.
     *
     * @param serviceSetting serviceSettings
     */
    public WMSMapSwing(ServiceSettings serviceSetting) {
        initGeotoolsLocale();
        init(serviceSetting);
        initBboxCoordinates();
    }

    private void init(ServiceSettings serviceSetting) {
        try {
            createMapView();
            initWmsAndLayer(serviceSetting);
            setMapCRS(CRS.decode(INITIAL_CRS));

            this.sb = new StyleBuilder();
            VBox vBox = new VBox();
            vBox.getChildren().add(createToolbar());
            vBox.getChildren().add(this.mapView);
            this.mapView.resize(MAP_WIDTH, MAP_HEIGHT);
            vBox.resize(MAP_WIDTH, MAP_HEIGHT);
            this.getChildren().add(vBox);

            String wmsSource = serviceSetting.getWMSSource();
            if (wmsSource != null) {
                Text sourceLabel = new Text(wmsSource);
                //mapHeight += SOURCE_LABEL_HEIGHT;
                vBox.getChildren().add(sourceLabel);
            }

            //Projection.WGS_84, true);
            this.mapView.initialize();
        } catch (FactoryException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void initBboxCoordinates() {
        bboxCoordinates = new BboxCoordinates();
        try {
            bboxCoordinates.setDisplayCRS(INITIAL_CRS);
        } catch (FactoryException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * sets text fields for coordinates.
     *
     * @param textX1 x1
     * @param textX2 y1
     * @param textY1 x2
     * @param textY2 y2
     */
    public void setCoordinateDisplay(
        TextField textX1,
        TextField textX2,
        TextField textY1,
        TextField textY2) {
        bboxCoordinates.setCoordinateDisplay(textX1, textX2, textY1, textY2);
    }

    /**
     * Sets the Labels.
     *
     * @param labelx1 label x1
     * @param labelx2 label x2
     * @param labely1 label y1
     * @param labely2 label y2
     */
    public void setCoordinateLabel(
        Label labelx1,
        Label labelx2,
        Label labely1,
        Label labely2
    ) {
        bboxCoordinates.setCoordinateLabel(labelx1, labelx2, labely1, labely2);
    }

    private void initWmsAndLayer(ServiceSettings serviceSetting) {
        try {
            String wmsLayerName = serviceSetting.getWMSLayer();
            this.wms = new WebMapServer(new URL(serviceSetting.getWMSUrl()));
            List<Layer> layers = this.wms.getCapabilities().getLayerList();
            Layer baseLayer = null;
            boolean layerFound = false;
            for (Layer outerLayer : layers) {
                String oname = outerLayer.getName();
                if (oname != null && oname.equalsIgnoreCase(wmsLayerName)) {
                    baseLayer = outerLayer;
                    // we actually need to set both by hand, else the
                    // request will fail
                    baseLayer.setTitle(wmsLayerName);
                    baseLayer.setName(wmsLayerName);
                    layerFound = true;
                }
                for (Layer wmsLayer : outerLayer.getChildren()) {
                    if (wmsLayer.getName().equalsIgnoreCase(wmsLayerName)) {
                        baseLayer = wmsLayer.getParent();
                        baseLayer.setTitle(wmsLayerName);
                        baseLayer.setName(wmsLayerName);
                        layerFound = true;
                        break;
                    }
                }
                if (layerFound) {
                    break;
                }
            }
            this.mapContent = new MapContent();
            this.polygonsOnMapViewHandler =
                new PolygonsOnMapViewHandler(this.mapView);
            this.mapContent.addMapLayerListListener(
                polygonsOnMapViewHandler);
            displayMap(baseLayer);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void createMapView() {
        this.mapView = new MapView();
        mapView.initializedProperty().addListener((observable,
                                                   oldValue,
                                                   newValue) -> {
            if (newValue) {
                afterMapIsInitialized();
            }
        });

        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            event.consume();
            if (bboxButton.isSelected()) {
                handleBboxClickEvent(event);
            } else if (infoButton.isSelected()) {
                handleInfoClickedEvent(event);
            }
        });
    }

    private void handleBboxClickEvent(MapViewEvent event) {
        if (mapContent.layers().size() == 1) {
            if (currentBbox != null) {
                bboxFirst = null;
                mapView.removeCoordinateLine(currentBbox);
                currentBbox = null;
            }
            if (bboxFirst == null) {
                bboxFirst = event.getCoordinate();
            } else {
                Coordinate bboxSecond = event.getCoordinate();
                double x1 = bboxFirst.getLongitude();
                double x2 = bboxSecond.getLongitude();
                double y1 = bboxFirst.getLatitude();
                double y2 = bboxSecond.getLatitude();
                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2);
                double minY = Math.min(y1, y2);
                double maxY = Math.max(y1, y2);

                Coordinate lowerLeft = new Coordinate(minY, minX);
                Coordinate upperLeft = new Coordinate(maxY, minX);
                Coordinate upperRight = new Coordinate(maxY, maxX);
                Coordinate lowerRight = new Coordinate(minY, maxX);

                currentBbox = new CoordinateLine(lowerLeft, upperLeft,
                    upperRight, lowerRight)
                    .setWidth(2)
                    .setColor(javafx.scene.paint.Color.DARKRED)
                    .setClosed(true)
                    .setVisible(true);
                mapView.addCoordinateLine(currentBbox);

                bboxCoordinates.setDisplayCoordinates(minX, maxX, minY, maxY,
                    mapCRS);
            }
        } else {
            Coordinate clickedCoord = event.getCoordinate();
            DirectPosition2D pos = new DirectPosition2D(
                clickedCoord.getLatitude(), clickedCoord.getLongitude());
            for (org.geotools.map.Layer mapLayer : mapContent.layers()) {
                if (mapLayer.isSelected()) {
                    String layerName = detectLayerName(mapLayer);
                    InfoToolHelper helper =
                        InfoToolHelperLookup.getHelper(mapLayer);
                    if (helper == null) {
                        LOG.warn("InfoTool cannot query {0}",
                            mapLayer.getClass().getName());
                        return;
                    }

                    helper.setMapContent(mapContent);
                    helper.setLayer(mapLayer);

                    highlightClickedPolygon(pos, layerName, helper);
                }
            }
        }
    }

    private void handleInfoClickedEvent(MapViewEvent event) {
        Coordinate clickedCoord = event.getCoordinate();
        DirectPosition2D pos = new DirectPosition2D(
            clickedCoord.getLatitude(), clickedCoord.getLongitude());
        featureInfoReporter.createReporter();
        featureInfoReporter.report(pos);

        for (org.geotools.map.Layer mapLayer : mapContent.layers()) {
            if (mapLayer.isSelected()) {
                String layerName = detectLayerName(mapLayer);
                InfoToolHelper helper =
                    InfoToolHelperLookup.getHelper(mapLayer);
                if (helper == null) {
                    LOG.warn("InfoTool cannot query {0}",
                        mapLayer.getClass().getName());
                    return;
                }

                helper.setMapContent(mapContent);
                helper.setLayer(mapLayer);

                try {
                    InfoToolResult result = helper.getInfo(pos);
                    featureInfoReporter.report(layerName, result);
                } catch (Exception var11) {
                    LOG.warn("Unable to query layer {}", layerName);
                }
            }
        }
    }


    private void highlightClickedPolygon(DirectPosition2D pos,
                                         String layerName,
                                         InfoToolHelper helper) {
        try {
            if (layerName.equals(POLYGON_LAYER_TITLE)) {
                InfoToolResult result =
                    helper.getInfo(pos);
                int numFeatures =
                    result.getNumFeatures();
                if (numFeatures == 1) {
                    Map<String, Object> featureData
                        = result.
                        getFeatureData(0);
                    String name = (String)
                        featureData.get(
                            "name");
                    String id = (String)
                        featureData.get("id");
                    setNameAndId(name, id);
                } else if (numFeatures > 1) {
                    //Yup, this is dirty.
                    setNameAndId("#@#", Integer
                        .toString(numFeatures));
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void setNameAndId(String name, String id) {
        PolygonInfos polyInf = new PolygonInfos(name, id);
        Platform.runLater(
            () -> fireEvent(new PolygonClickedEvent(polyInf)));
    }

    private String detectLayerName(org.geotools.map.Layer mapLayer) {
        String layerName = mapLayer.getTitle();
        FeatureSource<?, ?> featureSource = mapLayer.getFeatureSource();
        if (layerName == null || layerName.isEmpty()) {
            layerName = featureSource.getName().getLocalPart();
        }
        if (layerName == null || layerName.isEmpty()) {
            layerName = featureSource.getSchema().getName().getLocalPart();
        }
        return layerName;
    }

    private void afterMapIsInitialized() {
        LOG.debug("Initialize map");
        ServiceSettings serviceSetting = Config.getInstance().getServices();
        mapView.setWMSParam(new WMSParam()
            .setUrl(serviceSetting.getWMSUrl())
            .addParam("layers", serviceSetting.getWMSLayer()));
        mapView.setMapType(MapType.WMS);
        setInitialExtend();
        LOG.debug("initialization of " + mapView.toString() + " finished");
    }

    private ToolBar createToolbar() {
        ToggleGroup bboxOrInfoGroup = new ToggleGroup();
        this.bboxButton = new ToggleButton(TOOLBAR_POINTER_BUTTON_NAME);
        bboxButton.setToggleGroup(bboxOrInfoGroup);
        this.infoButton = new ToggleButton(TOOLBAR_INFO_BUTTON_NAME);
        infoButton.setToggleGroup(bboxOrInfoGroup);

        Button resizeButton = new Button(TOOLBAR_RESET_BUTTON_NAME);
        resizeButton.setOnAction(event -> setInitialExtend());

        ToolBar toolBar = new ToolBar(bboxButton, infoButton, resizeButton);
        toolBar.setOrientation(Orientation.HORIZONTAL);
        return toolBar;
    }

    /**
     * return the Bounds of the Map.
     *
     * @return the Bounds of the Map
    public Envelope2D getBounds() {
    return getBounds(this.displayCRS);
    }
     */

    /**
     * Set display CRS.
     *
     * @param crs crs
     */
    public void setDisplayCRS(CoordinateReferenceSystem crs) {
        this.bboxCoordinates.setDisplayCRS(crs);
    }

    /**
     * Set display CRS.
     *
     * @param crs crs
     * @throws FactoryException when the CRS can't be found
     */
    public void setDisplayCRS(String crs) throws FactoryException {
        this.bboxCoordinates.setDisplayCRS(crs);
    }

    private void setMapCRS(CoordinateReferenceSystem crs) {
        this.mapCRS = crs;
    }


    private void displayMap(Layer wmsLayer) {
        CRSEnvelope targetEnv = null;
        for (CRSEnvelope env : wmsLayer.getLayerBoundingBoxes()) {
            if (env.getEPSGCode().equals(INITIAL_CRS)) {
                targetEnv = env;
            }
        }
        wmsLayer.setBoundingBoxes(targetEnv);

        this.layer = new WMSLayer(this.wms, wmsLayer);
        this.mapContent.addLayer(this.layer);
        setMapCRS(this
            .mapContent
            .getViewport()
            .getCoordinateReferenceSystem());
        // createSwingContent(this.mapNode);
    }

    /**
     * highlight the selected Polygon.
     *
     * @param polygonID the selected Polygon
     */
    public void highlightSelectedPolygon(String polygonID) {
        this.polygonsOnMapViewHandler.highlightSelectedPolygon(polygonID);
    }

    /**
     * Resizes swing content and centers map.
     *
     * @param width The new content width.
     */
    public void resizeSwingContent(double width) {
        /*
        try {
            if (width >= mapWidth) {
                double oldWidth = mapPane.getWidth();

                this.mapNode.resize(width - MAP_NODE_MARGIN, mapHeight);
                double scale = mapPane.getWorldToScreenTransform().getScaleX();
                ReferencedEnvelope bounds = mapPane.getDisplayArea();

                double dXScreenCoord = (width - MAP_NODE_MARGIN - oldWidth) / 2;
                double dXWorldCoord = dXScreenCoord / scale;

                bounds.translate(-1 * dXWorldCoord , 0);
                mapPane.setDisplayArea(bounds);
                mapPane.deleteGraphics();
                clearCoordinateDisplay();
            }
        } catch (NullPointerException e) { }
        */
    }

    /**
     * repaints the map.
     */
    public void repaint() {
        /*
        Task task = new Task() {
            protected Integer call() {
                mapPane.repaint();
                return 0;
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        */
    }

    /**
     * Checks status code of a getMap request, using a head request.
     * Workaround to get geotools getMap request status codes.
     *
     * @param requestURL getMap URL
     * @return Status string containing status code, reason phrase and method
     */
    private String checkGetMap(String requestURL) {
        try (
            CloseableHttpClient client =
                HTTP.getClient(new URL(requestURL), null, null);
        ) {
            HttpHead head = new HttpHead(requestURL);
            CloseableHttpResponse resp = client.execute(head);
            return resp.getStatusLine().getStatusCode() + " "
                + resp.getStatusLine().getReasonPhrase() + " "
                + "GET";
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return "";
    }

    /**
     * Draws Polygons on the maps.
     *
     * @param featurePolygons List of drawable Polygons
     */
    public void drawPolygons(List<FeaturePolygon> featurePolygons) {
        try {
            String epsgCode = this
                .mapCRS
                .getIdentifiers()
                .toArray()[0]
                .toString();
            epsgCode = epsgCode.substring(epsgCode.lastIndexOf(':') + 1,
                epsgCode.length());
            SimpleFeatureType polygonFeatureType = DataUtilities.createType(
                "Dataset",
                "geometry:Polygon:srid="
                    + epsgCode
                    + ","
                    + "name:String,"
                    + "id:String"
            );
            polygonFeatureCollection =
                new DefaultFeatureCollection("internal",
                    polygonFeatureType);

            for (FeaturePolygon fp : featurePolygons) {
                SimpleFeatureBuilder featureBuilder =
                    new SimpleFeatureBuilder(polygonFeatureType);
                try {
                    MathTransform transform = CRS.findMathTransform(
                        fp.getCrs(), this.mapCRS);
                    featureBuilder.add(JTS.transform(fp.getPolygon(),
                        transform));
                    featureBuilder.add(fp.getName());
                    featureBuilder.add(fp.getId());
                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    polygonFeatureCollection.add(feature);
                } catch (FactoryException | TransformException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            org.geotools.map.Layer polygonLayer = new FeatureLayer(
                polygonFeatureCollection, sb.createStyle());
            polygonLayer.setTitle(POLYGON_LAYER_TITLE);
            removePolygonLayer();
            mapContent.addLayer(polygonLayer);
        } catch (SchemaException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * sets the viewport of the map to the given extend.
     *
     * @param envelope the extend
     */
    public void setExtend(ReferencedEnvelope envelope) {
        try {
            envelope = envelope.transform(this.mapContent.getViewport()
                .getCoordinateReferenceSystem(), true);
            double xLength = envelope.getSpan(0);
            xLength = xLength * TEN_PERCENT;
            double yLength = envelope.getSpan(1);
            yLength = yLength * TEN_PERCENT;
            envelope.expandBy(xLength, yLength);

            DirectPosition lowerCorner = envelope.getLowerCorner();
            DirectPosition upperCorner = envelope.getUpperCorner();
            Coordinate lower = new Coordinate(lowerCorner.getOrdinate(0),
                lowerCorner.getOrdinate(1));
            Coordinate upper = new Coordinate(upperCorner.getOrdinate(0),
                upperCorner.getOrdinate(1));
            mapView.setExtent(Extent.forCoordinates(lower, upper));

            if (currentBbox != null) {
                mapView.removeCoordinateLine(currentBbox);
            }
        } catch (FactoryException | TransformException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void setInitialExtend() {
        try {
            CoordinateReferenceSystem coordinateReferenceSystem =
                CRS.decode(INITIAL_CRS);
            ReferencedEnvelope initExtend =
                new ReferencedEnvelope(
                    BAY_MIN_LAT,
                    BAY_MAX_LAT,
                    BAY_MIN_LON,
                    BAY_MAX_LON,
                    coordinateReferenceSystem);
            setExtend(initExtend);
        } catch (FactoryException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    /**
     * return the Bounds of the Map.
     *
     * @param crs the CRS of the Bounding Box
     * @return the Bounds of the Map
     */
    public Envelope2D getBounds(CoordinateReferenceSystem crs) {
        return this.bboxCoordinates.getBounds(crs);
    }


    /**
     * resets the map.
     */
    public void reset() {
        this.bboxCoordinates.clearCoordinateDisplay();
        removePolygonLayer();
        this.polygonFeatureCollection = null;
    }

    private void removePolygonLayer() {
        this.mapContent.layers().stream()
            .filter(layer -> layer.getTitle() != null)
            .filter(layer -> layer.getTitle().equals(POLYGON_LAYER_TITLE))
            .forEach(layer -> mapContent.removeLayer(layer));
    }

}
