package de.bayern.gdi.gui.map;

import com.sothawo.mapjfx.MapView;
import de.bayern.gdi.utils.ServiceSettings;
import javafx.event.EventTarget;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */


public class WmsHandlerBuilder {

    private static ServiceSettings serviceSettings;
    private BboxCoordinates bboxCoordinates;
    private EventTarget mapNode;
    private MapView mapView;
    private MapActionToolbar mapActionToolbar;

    public static WmsHandlerBuilder newBuilder(ServiceSettings serviceSettings) {
        WmsHandlerBuilder.serviceSettings = serviceSettings;
        return new WmsHandlerBuilder();
    }

    public WmsHandlerBuilder with(EventTarget mapNode) {
        this.mapNode = mapNode;
        return this;
    }

    public WmsHandlerBuilder with(MapView mapView) {
        this.mapView = mapView;
        return this;
    }

    public WmsHandlerBuilder with(Label wmsSourceLabel) {
        String wmsSource = serviceSettings.getWMSSource();
        if (wmsSource != null) {
            wmsSourceLabel.setText(wmsSource);
        }
        return this;
    }

    public WmsHandlerBuilder withBboxButton(ToggleButton bboxButton) {
        initToolbar();
        this.mapActionToolbar.setBboxButton(bboxButton);
        return this;
    }

    public WmsHandlerBuilder withInfoButton(ToggleButton infoButton) {
        initToolbar();
        this.mapActionToolbar.setInfoButton(infoButton);
        return this;
    }

    public WmsHandlerBuilder withResizeButtton(Button resizeButton) {
        initToolbar();
        this.mapActionToolbar.setResizeButtton(resizeButton);
        return this;
    }


    public WmsHandlerBuilder withSelectButton(ToggleButton selectButton) {
        initToolbar();
        this.mapActionToolbar.setSelectButtton(selectButton);
        return this;
    }

    /**
     * sets text fields for coordinates.
     *
     * @param textX1 x1
     * @param textX2 y1
     * @param textY1 x2
     * @param textY2 y2
     */
    public WmsHandlerBuilder withCoordinateDisplay(
        TextField textX1,
        TextField textX2,
        TextField textY1,
        TextField textY2) {
        initBboxCoordinates();
        bboxCoordinates.setCoordinateDisplay(textX1, textX2, textY1, textY2);
        return this;
    }

    /**
     * Sets the Labels.
     *
     * @param labelx1 label x1
     * @param labelx2 label x2
     * @param labely1 label y1
     * @param labely2 label y2
     */
    public WmsHandlerBuilder withCoordinateLabel(
        Label labelx1,
        Label labelx2,
        Label labely1,
        Label labely2
    ) {
        initBboxCoordinates();
        bboxCoordinates.setCoordinateLabel(labelx1, labelx2, labely1, labely2);
        return this;
    }

    public WMSMapSwing build() {
        WMSMapSwing wmsMapHandler = new WMSMapSwing(serviceSettings, mapNode, mapView, bboxCoordinates, mapActionToolbar);
        this.mapActionToolbar.registerResizeHandler(wmsMapHandler);
        return wmsMapHandler;
    }


    private void initBboxCoordinates() {
        if (this.bboxCoordinates == null) {
            this.bboxCoordinates = new BboxCoordinates();
        }
    }

    private void initToolbar() {
        if (this.mapActionToolbar == null) {
            this.mapActionToolbar = new MapActionToolbar();
        }
    }
}
