package de.bayern.gdi.gui;

import de.bayern.gdi.model.MIMEType;
import de.bayern.gdi.model.MIMETypes;
import de.bayern.gdi.model.ProcessingStepConfiguration;
import de.bayern.gdi.utils.Config;
import de.bayern.gdi.utils.DownloadConfig;
import de.bayern.gdi.utils.I18n;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class ProcessingChainController {

    private static final String STATUS_READY = "status.ready";

    private static final String OUTPUTFORMAT = "outputformat";

    private static final String FX_BORDER_COLOR_NULL
        = "-fx-border-color: null;";

    private static final String FX_BORDER_COLOR_RED = "-fx-border-color: red;";

    private static final String GUI_PROCESS_NO_FORMAT
        = "gui.process.no.format";

    private static final String GUI_PROCESS_FORMAT_NOT_FOUND
        = "gui.process.format.not.found";

    private static final String GUI_PROCESS_NOT_COMPATIBLE
        = "gui.process.not.compatible";

    @Inject
    private StatusLogController statusLogController;

    @Inject
    private Controller controller;

    @FXML
    private VBox chainContainer;

    @FXML
    private CheckBox chkChain;

    @FXML
    private HBox processStepContainter;

    @FXML
    private Button addChainItem;

    private UIFactory factory = new UIFactory();

    /**
     * Handle events on the process Chain Checkbox.
     *
     * @param event
     *     the event
     */
    @FXML
    protected void handleChainCheckbox( ActionEvent event ) {
        if ( chkChain.isSelected() ) {
            processStepContainter.setVisible( true );
        } else {
            factory.removeAllChainAttributes( chainContainer );
            processStepContainter.setVisible( false );
        }
    }

    /**
     * Handle the dataformat selection.
     *
     * @param event
     *     The event
     */
    @FXML
    protected void handleAddChainItem( ActionEvent event ) {
        factory.addChainAttribute( chainContainer,
                                   this::validateChainContainerItems );
        validateChainContainerItems();
    }

    public void setVisible( boolean isVisible ) {
        this.processStepContainter.setVisible( isVisible );
    }

    public void setProcessingSteps( List<DownloadConfig.ProcessingStep> steps ) {
        factory.removeAllChainAttributes( chainContainer );
        if ( steps != null ) {
            chkChain.setSelected( true );
            handleChainCheckbox( new ActionEvent() );

            for ( DownloadConfig.ProcessingStep iStep : steps ) {
                factory.addChainAttribute( chainContainer,
                                           iStep.getName(), iStep.getParams() );
            }
        } else {
            chkChain.setSelected( false );
            handleChainCheckbox( new ActionEvent() );
        }
    }

    public Set<Node> getProcessingChainParameter() {
        if ( !this.chkChain.isSelected() ) {
            Collections.emptyList();
        }
        return this.chainContainer.lookupAll( "#process_parameter" );
    }

    /**
     * Resets all marks at the processing chain container, items are kept.
     */
    public void resetProcessingChainContainer() {
        for ( Node o : chainContainer.getChildren() ) {
            if ( o instanceof VBox ) {
                VBox v = (VBox) o;
                HBox hbox = (HBox) v.getChildren().get( 0 );
                Node cBox = hbox.getChildren().get( 0 );
                if ( cBox instanceof ComboBox ) {
                    cBox.setStyle( FX_BORDER_COLOR_NULL );
                    ComboBox box = (ComboBox) cBox;
                    ObservableList<ProcessingStepConfiguration> confs =
                        (ObservableList<ProcessingStepConfiguration>)
                            box.getItems();
                    for ( ProcessingStepConfiguration cfgI : confs ) {
                        cfgI.setCompatible( true );
                        confs.set( confs.indexOf( cfgI ), cfgI );
                    }
                }
            }
        }
    }

    /**
     * Validates all items in processing chain container.
     */
    public void validateChainContainerItems() {

        boolean allValid = true;
        for ( Node o : chainContainer.getChildren() ) {
            if ( o instanceof VBox ) {
                VBox v = (VBox) o;
                HBox hbox = (HBox) v.getChildren().get( 0 );
                Node cBox = hbox.getChildren().get( 0 );
                if ( cBox instanceof ComboBox
                     && !validateChainContainer( (ComboBox) cBox ) ) {
                    allValid = false;
                }
            }
        }
        //If all chain items were ready, set status to ready
        if ( allValid ) {
            statusLogController.setStatusTextUI( I18n.format( STATUS_READY ) );
        }
    }

    /**
     * Validates the chain items of a ComboBox
     * and marks the box according to the chosen item.
     *
     * @param box
     *     Item to validate
     * @return True if chosen item is valid, else false
     */
    private boolean validateChainContainer( ComboBox box ) {
        String format = controller.dataBean.getAttributeValue( OUTPUTFORMAT );
        if ( format == null ) {
            box.setStyle( FX_BORDER_COLOR_RED );
            statusLogController.setStatusTextUI( I18n.format( GUI_PROCESS_NO_FORMAT ) );
        }
        MIMETypes mtypes = Config.getInstance().getMimeTypes();
        MIMEType mtype = mtypes.findByName( format );

        ProcessingStepConfiguration cfg =
            (ProcessingStepConfiguration) box.getValue();
        ObservableList<ProcessingStepConfiguration> items =
            (ObservableList<ProcessingStepConfiguration>) box.getItems();

        if ( format != null && mtype == null ) {
            box.setStyle( FX_BORDER_COLOR_RED );
            for ( ProcessingStepConfiguration cfgI : items ) {
                cfgI.setCompatible( false );
                //Workaround to force cell update
                items.set( items.indexOf( cfgI ), cfgI );
            }
            statusLogController.setStatusTextUI( I18n.format( GUI_PROCESS_FORMAT_NOT_FOUND ) );
            return false;
        }

        //Mark items that are incompatible
        for ( ProcessingStepConfiguration cfgI : items ) {
            if ( format != null ) {
                cfgI.setCompatible(
                    cfgI.isCompatibleWithFormat( mtype.getType() ) );
            } else {
                cfgI.setCompatible( false );
            }
            items.set( items.indexOf( cfgI ), cfgI );
        }

        if ( format == null ) {
            return false;
        }

        if ( cfg == null ) {
            box.setStyle( FX_BORDER_COLOR_NULL );
            return true;
        }

        if ( cfg.isCompatible() ) {
            box.setStyle( FX_BORDER_COLOR_NULL );
        } else {
            box.setStyle( FX_BORDER_COLOR_RED );
            statusLogController.setStatusTextUI( I18n.format( GUI_PROCESS_NOT_COMPATIBLE,
                                                              box.getValue() ) );
        }
        return cfg.isCompatible();
    }
}
