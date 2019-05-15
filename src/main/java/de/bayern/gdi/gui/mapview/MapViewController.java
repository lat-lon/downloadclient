package de.bayern.gdi.gui.mapview;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Projection;
import com.sothawo.mapjfx.WMSParam;
import de.bayern.gdi.utils.Config;
import de.bayern.gdi.utils.ServiceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class MapViewController {

    private static final Logger log
        = LoggerFactory.getLogger(MapViewController.class.getName());

    /**
     * Default zoom value.
     */
    private static final int ZOOM_DEFAULT = 14;

    private final MapView mapView;

    public MapViewController(MapView mapView) {
        this.mapView = mapView;
    }

    public void initializeMapView() {
        log.debug("starting MapView initialization");
        ServiceSettings serviceSetting = Config.getInstance().getServices();
        mapView.initialize(Projection.WGS_84, true);
        mapView.setZoom(ZOOM_DEFAULT);
        mapView.setAnimationDuration(0);
        mapView.setMapType(MapType.OSM);
        mapView.setWMSParam(new WMSParam()
            .setUrl(serviceSetting.getWMSUrl())
            .addParam("layers", serviceSetting.getWMSLayer()));
        mapView.setCenter(new Coordinate(48.136923, 11.591207));
        mapView.toFront();
        log.debug("initialization of " + mapView.toString() + " finished");
    }

}
