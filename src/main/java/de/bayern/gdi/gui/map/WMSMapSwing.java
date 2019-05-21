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
import javafx.embed.swing.SwingNode;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.geotools.data.DataUtilities;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.factory.CommonFactoryFinder;
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
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.dialog.JTextReporter;
import org.geotools.swing.dialog.TextReporterListener;
import org.geotools.swing.locale.LocaleUtils;
import org.geotools.swing.tool.InfoToolHelper;
import org.geotools.swing.tool.InfoToolResult;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jochen Saalfeld (jochen@intevation.de)
 */

public class WMSMapSwing extends Parent {

    private static final Logger LOG
        = LoggerFactory.getLogger(WMSMapSwing.class.getName());

    private static final int MAP_WIDTH = 400;
    private static final int MAP_HEIGHT = 250;

    /**
     * Default zoom value.
     */
    private static final int ZOOM_DEFAULT = 14;
    private WebMapServer wms;

    private MapView mapView;

    private int mapWidth;
    private int mapHeight;
    private SwingNode mapNode;
    private StyleBuilder sb;
    private StyleFactory sf;
    private FilterFactory2 ff;
    DefaultFeatureCollection polygonFeatureCollection;
    private CoordinateReferenceSystem mapCRS;

    private static final double TEN_PERCENT = 0.1D;
    private static final String POLYGON_LAYER_TITLE = "PolygonLayer";
    private static final String TOOLBAR_INFO_BUTTON_NAME = "ToolbarInfoButton";
    private static final String TOOLBAR_POINTER_BUTTON_NAME
        = "ToolbarPointerButton";
    private static final String TOOLBAR_RESET_BUTTON_NAME
        = "ToolbarResetButton";

    private static final double INITIAL_EXTEND_X1 = 850028;
    private static final double INITIAL_EXTEND_Y1 = 6560409;
    private static final double INITIAL_EXTEND_X2 = 1681693;
    private static final double INITIAL_EXTEND_Y2 = 5977713;
    private static final String INITIAL_CRS = "EPSG:4326";

    private static final int MAP_NODE_MARGIN = 40;
    private static final int SOURCE_LABEL_HEIGHT = 70;

    private static final Color OUTLINE_COLOR = Color.BLACK;
    private static final Color SELECTED_COLOUR = Color.YELLOW;
    private static final Color FILL_COLOR = Color.CYAN;
    private static final Float OUTLINE_WIDTH = 0.3f;
    private static final Float FILL_TRANSPARACY = 0.4f;
    private static final Float STROKY_TRANSPARACY = 0.8f;
    private GeometryDescriptor geomDesc;
    private String geometryAttributeName;
    private String source;

    private static final Coordinate CENTER_BAY = new Coordinate(48.136923, 11.591207);
    private static final Extent EXTENT_BAY = Extent.forCoordinates(new Coordinate(50.654523743525, 7.63593144329548), new Coordinate(47.2178956772476, 15.1069052509681));
    private ToggleButton infoButton;
    private ToggleButton bboxButton;

    private Coordinate bboxFirst;
    private Coordinate bboxSecond;
    private CoordinateLine bbox;
    private WMSLayer layer;
    private MapContent mapContent;
    private JTextReporter.Connection textReporterConnection;
    private CoordinateLine currentBbox;
    private BboxCoordinates bboxCoordinates;


    /**
     * Initializes geotools localisation system with our default I18n locale.
     * TODO: Buggy with org/geotools/gt-swing/14.3 (and probably also w 15.0.)
     */
    private static void initGeotoolsLocale() {
        LocaleUtils.setLocale(I18n.getLocale());
    }

    /**
     * adds a node to this map.
     *
     * @param n the node
    public void add(Node n) {
    this.vBox.getChildren().remove(n);
    this.vBox.getChildren().add(n);
    }
     */

    /**
     * Constructor.
     */
    public WMSMapSwing() {
        initGeotoolsLocale();
    }

    /**
     * Constructor.
     *
     * @param mapURL mapURL
     * @param width  width
     * @param height height
     * @param layer  layer
     * @param mapURL The URL of the WMS Service
     * @throws MalformedURLException
    public WMSMapSwing(String mapURL, int width, int height, String layer)
    throws
    MalformedURLException {
    this(new URL(mapURL), width, height, layer);
    }
     */

    /**
     * Constructor.
     *
     * @param mapURL mapURL
     * @param width  width
     * @param height height
     * @param layer  layer
    public WMSMapSwing(URL mapURL, int width, int height, String layer) {
    this(mapURL, width, height, layer, null, null);
    }
     */

    /**
     * Constructor.
     *
     * @param mapURL mapURL
     * @param width  width
     * @param height height
     * @param layer  layer
     * @param source source
    public WMSMapSwing(URL mapURL, int width, int height, String layer,
    String source) {
    this(mapURL, width, height, layer, null, source);
    }

     */
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
     * @param mapURL     mapURL
     * @param width      width
     * @param height     height
     * @param layer      layer
     * @param source     source
     * @param displayCRS crs of display
     */


    /**
     * Constructor.
     *
     * @param serviceSetting serviceSettings
     */
    public WMSMapSwing(ServiceSettings serviceSetting) {
        initGeotoolsLocale();
        init(serviceSetting);
        this.bboxCoordinates = new BboxCoordinates();
    }

    /**
     * Constructor.
     *
     * @param serviceSetting serviceSettings
     * @param textX1         textX1
     * @param textX2         textX2
     * @param textY1         textY1
     * @param textY2         textY2
     * @param labelX1        lableX1
     * @param labelX2        lableX2
     * @param labelY1        lableY1
     * @param labelY2        lableY2
     */
    public WMSMapSwing(ServiceSettings serviceSetting, TextField textX1, TextField textX2, TextField textY1, TextField textY2, Label labelX1, Label labelX2, Label labelY1, Label labelY2) {
        initGeotoolsLocale();
        init(serviceSetting);
        initBboxCoordinates(textX1, textX2, textY1, textY2, labelX1, labelX2, labelY1, labelY2);
    }

    private void init(ServiceSettings serviceSetting) {
        try {
            String layer = serviceSetting.getWMSLayer();
            String source = serviceSetting.getWMSSource();
            initWmsAndLayer(layer, serviceSetting);
            setMapCRS(CRS.decode(INITIAL_CRS));

            this.source = source;
            this.sb = new StyleBuilder();
            this.sf = CommonFactoryFinder.getStyleFactory(null);
            this.ff = CommonFactoryFinder.getFilterFactory2(null);
            this.mapWidth = MAP_WIDTH;
            this.mapHeight = MAP_HEIGHT;
            this.mapNode = new SwingNode();
            VBox vBox = new VBox();
            this.mapView = createMapView();
            ToolBar toolbar = createToolbar(this.mapView);
            vBox.getChildren().add(toolbar);
            vBox.getChildren().add(this.mapView);
            this.mapView.resize(MAP_WIDTH, MAP_HEIGHT);
            vBox.resize(MAP_WIDTH, MAP_HEIGHT);
            this.getChildren().add(vBox);

            this.mapView.initialize();//Projection.WGS_84, true);
        } catch (FactoryException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void initBboxCoordinates(TextField textX1, TextField textX2, TextField textY1, TextField textY2, Label labelX1, Label labelX2, Label labelY1, Label labelY2) {
        bboxCoordinates = new BboxCoordinates();
        try {
            bboxCoordinates.setDisplayCRS(INITIAL_CRS);
        } catch (FactoryException e) {
            LOG.error(e.getMessage(), e);
        }
        bboxCoordinates.setCoordinateDisplay(textX1, textX2, textY1, textY2);
        bboxCoordinates.setCoordinateLabel(labelX1, labelX2, labelY1, labelY2);
    }

    private void initWmsAndLayer(String layer, ServiceSettings serviceSetting) {
        try {
            this.wms = new WebMapServer(new URL(serviceSetting.getWMSUrl()));
            List<Layer> layers = this.wms.getCapabilities().getLayerList();
            Layer baseLayer = null;
            boolean layerFound = false;
            for (Layer outerLayer : layers) {
                String oname = outerLayer.getName();
                if (oname != null && oname.equalsIgnoreCase(layer)) {
                    baseLayer = outerLayer;
                    // we actually need to set both by hand, else the
                    // request will fail
                    baseLayer.setTitle(layer);
                    baseLayer.setName(layer);
                    layerFound = true;
                }
                for (Layer wmsLayer : outerLayer.getChildren()) {
                    if (wmsLayer.getName().equalsIgnoreCase(layer)) {
                        baseLayer = wmsLayer.getParent();
                        baseLayer.setTitle(layer);
                        baseLayer.setName(layer);
                        layerFound = true;
                        break;
                    }
                }
                if (layerFound) {
                    break;
                }
            }
            this.mapContent = new MapContent();
            displayMap(baseLayer);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private MapView createMapView() {
        MapView mapView = new MapView();
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                afterMapIsInitialized();
            }
        });

        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            event.consume();
            if (bboxButton.isSelected()) {
                handleBboxClickEvent(mapView, event);
            } else if (infoButton.isSelected()) {
                handleInfoClickedEvent(event);
            }
        });


        return mapView;
    }

    private void handleBboxClickEvent(MapView mapView, MapViewEvent event) {
        if (currentBbox != null) {
            mapView.removeCoordinateLine(currentBbox);
        }
        if (bboxFirst == null) {
            bboxFirst = event.getCoordinate();
        } else {
            bboxSecond = event.getCoordinate();
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

            currentBbox = new CoordinateLine(lowerLeft, upperLeft, upperRight, lowerRight).setWidth(2).setColor(javafx.scene.paint.Color.DARKRED).setClosed(true);
            mapView.addCoordinateLine(currentBbox);
            currentBbox.setVisible(true);

            bboxCoordinates.setDisplayCoordinates(minX, maxX, minY, maxY, mapCRS);
            bboxFirst = null;
            bboxSecond = null;
        }
    }

    private void handleInfoClickedEvent(MapViewEvent event) {
        Coordinate clickedCoord = event.getCoordinate();
        DirectPosition2D pos = new DirectPosition2D(clickedCoord.getLongitude(), clickedCoord.getLatitude());
        this.createReporter();
        this.report(pos);
        int nlayers = mapContent.layers().size();
        int n = 0;
        Iterator var6 = mapContent.layers().iterator();

        while (true) {
            org.geotools.map.Layer layer;
            do {
                if (!var6.hasNext()) {
                    this.textReporterConnection.appendSeparatorLine(10, '-');
                    this.textReporterConnection.appendNewline();
                    return;
                }

                layer = (org.geotools.map.Layer) var6.next();
            } while (!layer.isSelected());

            String layerName = layer.getTitle();
            if (layerName == null || layerName.length() == 0) {
                layerName = layer.getFeatureSource().getName().getLocalPart();
            }

            if (layerName == null || layerName.length() == 0) {
                layerName = layer.getFeatureSource().getSchema().getName().getLocalPart();
            }

            InfoToolHelper helper = InfoToolHelperLookup.getHelper(layer);
            if (helper == null) {
                LOG.warn("InfoTool cannot query {0}", layer.getClass().getName());
                return;
            }
            helper.setMapContent(mapContent);
            helper.setLayer(layer);

            try {
                InfoToolResult result = helper.getInfo(pos);
                this.textReporterConnection.append(layerName + "\n");
                this.textReporterConnection.append(result.toString(), 4);
                ++n;
                if (n < nlayers) {
                    this.textReporterConnection.append("\n");
                }
            } catch (Exception var11) {
                LOG.warn("Unable to query layer {}", layerName);
            }
        }
    }

    private void report(DirectPosition2D pos) {
        this.textReporterConnection.append(String.format("Pos x=%.4f y=%.4f\n", pos.x, pos.y));
    }

    private void createReporter() {
        if (this.textReporterConnection == null) {
            this.textReporterConnection = JTextReporter.showDialog("Feature info", null, 6, 20, 40);
            this.textReporterConnection.addListener(new TextReporterListener() {
                @Override
                public void onReporterClosed() {
                    textReporterConnection = null;
                }

                @Override
                public void onReporterUpdated() {

                }
            });
        }
    }

    private void afterMapIsInitialized() {
        LOG.debug("Initialize map");
        ServiceSettings serviceSetting = Config.getInstance().getServices();
        mapView.setZoom(ZOOM_DEFAULT);
        mapView.setAnimationDuration(0);
        mapView.setWMSParam(new WMSParam()
            .setUrl(serviceSetting.getWMSUrl())
            .addParam("layers", serviceSetting.getWMSLayer()));
        // TODO: should be WMS to show the WMS map
        // but currently drawing of the bbox does not work with MapType.WMS
        mapView.setMapType(MapType.OSM);
        mapView.setCenter(CENTER_BAY);
        LOG.debug("initialization of " + mapView.toString() + " finished");
    }

    private ToolBar createToolbar(MapView mapView) {
        ToggleGroup bboxOrInfoGroup = new ToggleGroup();
        this.bboxButton = new ToggleButton(TOOLBAR_POINTER_BUTTON_NAME);
        bboxButton.setToggleGroup(bboxOrInfoGroup);
        this.infoButton = new ToggleButton(TOOLBAR_INFO_BUTTON_NAME);
        infoButton.setToggleGroup(bboxOrInfoGroup);

        Button resizeButton = new Button(TOOLBAR_RESET_BUTTON_NAME);
        resizeButton.setOnAction(event -> mapView.setExtent(EXTENT_BAY));

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
     * sets name and id of the selected polygon.
     *
     * @param name name
     * @param id   id
     */
    private void setNameAndId(String name, String id) {
        PolygonInfos polyInf = new PolygonInfos(name, id);
        Platform.runLater(
            () -> fireEvent(new PolygonClickedEvent(polyInf)));
    }

    /**
     * highlight the selected Polygon.
     *
     * @param polygonID the selected Polygon
     */
    public void highlightSelectedPolygon(String polygonID) {
        /*
        for (SimpleFeature simpleFeature : polygonFeatureCollection) {
            String featureID = (String) simpleFeature.getAttribute("id");
            if (featureID.equals(polygonID)) {
                Style style =
                    createSelectedStyle(simpleFeature.getIdentifier());
                org.geotools.map.Layer layer = null;
                for (org.geotools.map.Layer layers : mapPane.getMapContent()
                        .layers()) {
                    String t = layers.getTitle();
                    if (t != null && t.equals(POLYGON_LAYER_TITLE)) {
                        layer = layers;
                    }
                }
                if (layer instanceof FeatureLayer) {
                    ((FeatureLayer) layer).setStyle(style);
                }
            }
        }
        */
    }

    /*
    private Style createSelectedStyle(FeatureId ids) {
        Rule selectedRule = createRule(SELECTED_COLOUR, SELECTED_COLOUR);
        selectedRule.setFilter(ff.id(ids));

        Rule otherRule = createRule(OUTLINE_COLOR, FILL_COLOR);
        otherRule.setElseFilter(true);

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        fts.rules().add(selectedRule);
        fts.rules().add(otherRule);

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    private Rule createRule(Color outlineColor, Color fillColor) {
        Symbolizer symbolizer = null;
        Fill fill = null;
        Stroke stroke = sf.createStroke(
                ff.literal(outlineColor),
                ff.literal(OUTLINE_WIDTH));

        fill = sf.createFill(ff.literal(fillColor),
                ff.literal(FILL_TRANSPARACY));
        symbolizer = sf.createPolygonSymbolizer(stroke, fill,
                geometryAttributeName);


        Rule rule = sf.createRule();
        rule.symbolizers().add(symbolizer);
        return rule;
    }
*/

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

    /*
    private void createSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[]");
            stringBuilder.append("[min!]");
            JPanel panel = new JPanel(new MigLayout(
                    "wrap 1, insets 0",
                    "[grow]",
                    stringBuilder.toString()));

            mapPane = new ExtJMapPane(mapContent);
            mapPane.setMinimumSize(new Dimension(mapWidth,
                    mapHeight));
            mapPane.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    mapPane.setBorder(
                            BorderFactory.createLineBorder(Color.BLACK));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    mapPane.setBorder(
                            BorderFactory.createLineBorder(
                                    Color.LIGHT_GRAY));
                }
            });
            mapPane.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    mapPane.requestFocusInWindow();
                }
            });

            //Add listener to log getMap requests after rendering
            mapPane.addMapPaneListener(new MapPaneListener() {
                @Override
                public void onDisplayAreaChanged(MapPaneEvent ev) {
                    // ignore me
                }

                @Override
                public void onNewMapContent(MapPaneEvent ev) {
                    // ignore me
                }

                @Override
                public void onRenderingStarted(MapPaneEvent ev) {
                    // ignore me
                }

                @Override
                public void onRenderingStopped(MapPaneEvent ev) {
                    String getMapUrl = wmslayer.getLastGetMap()
                            .getFinalURL().toString();
                    Controller.logToAppLog(checkGetMap(getMapUrl)
                            + " " + getMapUrl);
                }

            });
            JToolBar toolBar = new JToolBar();
            toolBar.setOrientation(JToolBar.HORIZONTAL);
            toolBar.setFloatable(false);
            JButton btn;
            JToggleButton tbtn;
            ButtonGroup cursorToolGrp = new ButtonGroup();
            ActionListener deleteGraphics = e -> mapPane.deleteGraphics();
            bboxAction = new CursorAction(mapPane);
            tbtn = new JToggleButton(bboxAction);
            tbtn.setName(TOOLBAR_POINTER_BUTTON_NAME);
            tbtn.addActionListener(deleteGraphics);
            toolBar.add(tbtn);
            cursorToolGrp.add(tbtn);
            tbtn = new JToggleButton(new ZoomInAction(mapPane));
            tbtn.addActionListener(deleteGraphics);
            tbtn.setName(TOOLBAR_ZOOMIN_BUTTON_NAME);
            toolBar.add(tbtn);
            cursorToolGrp.add(tbtn);
            tbtn = new JToggleButton(new ZoomOutAction(mapPane));
            tbtn.addActionListener(deleteGraphics);
            tbtn.setName(TOOLBAR_ZOOMOUT_BUTTON_NAME);
            toolBar.add(tbtn);
            cursorToolGrp.add(tbtn);
            toolBar.addSeparator();
            tbtn = new JToggleButton(new PanAction(mapPane));
            tbtn.addActionListener(deleteGraphics);
            tbtn.setName(TOOLBAR_PAN_BUTTON_NAME);
            toolBar.add(tbtn);
            cursorToolGrp.add(tbtn);
            toolBar.addSeparator();
            tbtn = new JToggleButton(new InfoAction(mapPane));
            tbtn.addActionListener(deleteGraphics);
            tbtn.setName(TOOLBAR_INFO_BUTTON_NAME);
            toolBar.add(tbtn);
            cursorToolGrp.add(tbtn);
            toolBar.addSeparator();
            btn = new JButton(new ResetAction(mapPane));
            btn.addActionListener(deleteGraphics);
            btn.setName(TOOLBAR_RESET_BUTTON_NAME);
            toolBar.add(btn);
            panel.add(toolBar, "grow");
            panel.add(mapPane, "grow");
            if (source != null) {
                JLabel sourceLabel = new JLabel(source);
                mapHeight += SOURCE_LABEL_HEIGHT;
                panel.add(sourceLabel, "grow");
            }
            swingNode.setContent(panel);
            setExtend(INITIAL_EXTEND_X1, INITIAL_EXTEND_X2,
                    INITIAL_EXTEND_Y1, INITIAL_EXTEND_Y2, INITIAL_CRS);
        });
    }
    */

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
            SimpleFeatureType polygonFeatureType;
            String epsgCode = this
                .mapCRS
                .getIdentifiers()
                .toArray()[0]
                .toString();
            epsgCode = epsgCode.substring(epsgCode.lastIndexOf(':') + 1,
                epsgCode.length());
            polygonFeatureType = DataUtilities.createType(
                "Dataset",
                "geometry:Geometry:srid="
                    + epsgCode
                    + ","
                    + "name:String,"
                    + "id:String"
            );
            polygonFeatureCollection =
                new DefaultFeatureCollection("internal",
                    polygonFeatureType);
            geomDesc = polygonFeatureCollection.getSchema()
                .getGeometryDescriptor();
            geometryAttributeName = geomDesc.getLocalName();

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
                polygonFeatureCollection, createPolygonStyle());
            polygonLayer.setTitle(POLYGON_LAYER_TITLE);
            List<org.geotools.map.Layer> layers = mapContent.layers();
            for (org.geotools.map.Layer layer : layers) {
                String t = layer.getTitle();
                if (t != null && t.equals(POLYGON_LAYER_TITLE)) {
                    mapContent.removeLayer(layer);
                }
            }
            mapContent.addLayer(polygonLayer);
        } catch (SchemaException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private Style createPolygonStyle() {
        Fill fill = sf.createFill(ff.literal(FILL_COLOR),
            ff.literal(FILL_TRANSPARACY));
        Stroke stroke = sf.createStroke(ff.literal(OUTLINE_COLOR),
            ff.literal(OUTLINE_WIDTH),
            ff.literal(STROKY_TRANSPARACY));
        PolygonSymbolizer polygonSymbolizer =
            sf.createPolygonSymbolizer(stroke, fill, null);
        return this.sb.createStyle(polygonSymbolizer);
    }

    /**
     * sets the viewport of the map to the given extend.
     *
     * @param envelope the extend
     */
    public void setExtend(ReferencedEnvelope envelope) {
        /*
        try {
            envelope = envelope.transform(this.mapContent.getViewport()
                    .getCoordinateReferenceSystem(), true);
            double xLength = envelope.getSpan(0);
            xLength = xLength * TEN_PERCENT;
            double yLength = envelope.getSpan(1);
            yLength = yLength * TEN_PERCENT;
            envelope.expandBy(xLength, yLength);
            //bboxAction.resetCoordinates();
            //mapPane.deleteGraphics();
            //mapPane.setDisplayArea(envelope);
        } catch (FactoryException | TransformException e) {
            LOG.error(e.getMessage(), e);
        }
        */
    }

    private void setExtend(Double x1, Double x2, Double y1, Double y2, String
        crs) {
        CoordinateReferenceSystem coordinateReferenceSystem = null;
        try {
            coordinateReferenceSystem = CRS.decode(crs);
            ReferencedEnvelope initExtend =
                new ReferencedEnvelope(x1,
                    x2,
                    y1,
                    y2, coordinateReferenceSystem);
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
        /*
        this.mapContent.layers().stream()
            .filter(layer -> layer.getTitle() != null)
            .filter(layer -> layer.getTitle().equals(POLYGON_LAYER_TITLE))
            .forEach(layer -> mapContent.removeLayer(layer));
            */
        this.polygonFeatureCollection = null;
        this.geomDesc = null;
        this.geometryAttributeName = null;
    }
}
