package de.bayern.gdi.gui.map;

import de.bayern.gdi.utils.I18n;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class MapActionToolbar {

    private ToggleButton bboxButton;

    private ToggleButton selectButton;

    private ToggleButton infoButton;

    private Button resizeButton;


    public void setSelectButtton(ToggleButton selectButton) {
        this.selectButton = selectButton;
        ImageView selectIcon = new ImageView(
            "/org/geotools/swing/icons/pointer.png");
        Tooltip selectTooltip = new Tooltip(I18n.format("tooltip.select"));
        this.selectButton.setGraphic(selectIcon);
        this.selectButton.setTooltip(selectTooltip);
    }

    public void setBboxButton(ToggleButton bboxButton) {
        this.bboxButton = bboxButton;
        ImageView bboxIcon = new ImageView(
            "/org/geotools/swing/icons/remove_layer.png");
        Tooltip bboxTooltip = new Tooltip(I18n.format("tooltip.bbox"));
        this.bboxButton.setGraphic(bboxIcon);
        this.bboxButton.setTooltip(bboxTooltip);
    }

    public void setInfoButton(ToggleButton infoButton) {
        this.infoButton = infoButton;
        ImageView infoIcon = new ImageView(
            "/org/geotools/swing/icons/mActionIdentify.png");
        Tooltip infoTooltip = new Tooltip(I18n.format("tooltip.info"));
        this.infoButton.setGraphic(infoIcon);
        this.infoButton.setTooltip(infoTooltip);
    }

    public void setResizeButtton(Button resizeButton) {
        this.resizeButton = resizeButton;
        ImageView resizeIcon = new ImageView(
            "/org/geotools/swing/icons/mActionZoomFullExtent.png");
        Tooltip resizeTooltip = new Tooltip(I18n.format("tooltip.resize"));
        this.resizeButton.setGraphic(resizeIcon);
        this.resizeButton.setTooltip(resizeTooltip);
    }

    public void registerResizeHandler(WMSMapSwing wmsMapSwing) {
        this.resizeButton.setOnAction(event -> wmsMapSwing.setInitialExtend());
    }

    public boolean isSelectButtonSelected() {
        if (selectButton != null) {
            return selectButton.isSelected();
        }
        return false;
    }

    public boolean isBboxButtonSelected() {
        if (bboxButton != null) {
            return bboxButton.isSelected();
        }
        return false;
    }

    public boolean isInfoButtonSelected() {
        if (infoButton != null) {
            return infoButton.isSelected();
        }
        return false;
    }
}
