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
package de.bayern.gdi.gui.controller;

import de.bayern.gdi.gui.WebViewWindow;
import de.bayern.gdi.config.Config;
import de.bayern.gdi.config.DownloadConfig;
import de.bayern.gdi.utils.I18n;
import de.bayern.gdi.utils.Misc;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;

import static de.bayern.gdi.gui.GuiConstants.USER_DIR;

/**
 * Menu bar controller.
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
@Named
@Singleton
public class MenuBarController {

    private static final Logger LOG = LoggerFactory.getLogger(MenuBarController.class.getName());

    @Inject
    private Controller controller;

    @Inject
    private ServiceSelectionController serviceSelectionController;

    @Inject
    private StatusLogController statusLogController;

    @FXML
    private MenuBar menuBar;

    /**
     * Handle action related to "About" menu item.
     *
     * @param event
     *     Event on "About" menu item.
     */
    @FXML
    private void handleAboutAction(final ActionEvent event) {
        try {
            String path = "about/about_"
                          + Locale.getDefault().getLanguage()
                          + ".html";
            displayHTMLFileAsPopup(I18n.getMsg("menu.about"), path);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Handle action related to "Help" menu item.
     *
     * @param event
     *     Event on "Help" menu item.
     */

    @FXML
    private void handleHelpAction(final ActionEvent event) {
        String pathToFile = "help/help_"
                            + Locale.getDefault().getLanguage()
                            + ".txt";
        try {
            openLinkFromFile(pathToFile);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Handler to close the application.
     *
     * @param event
     *     The event.
     */
    @FXML
    protected void handleCloseApp(ActionEvent event) {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        closeApp(stage);
    }

    /**
     * Closes app windows.
     * @param stage stage of application
     */
    public void closeApp(Stage stage) {
        Alert closeDialog = new Alert(Alert.AlertType.CONFIRMATION);
        closeDialog.setTitle(I18n.getMsg("gui.confirm-exit"));
        closeDialog.setContentText(I18n.getMsg("gui.want-to-quit"));
        ButtonType confirm = new ButtonType(I18n.getMsg("gui.exit"));
        ButtonType cancel = new ButtonType(I18n.getMsg("gui.cancel"),
                                            ButtonBar.ButtonData.CANCEL_CLOSE);
        closeDialog.getButtonTypes().setAll(confirm, cancel);
        Optional<ButtonType> res = closeDialog.showAndWait();
        if (res.isPresent() && res.get() == confirm) {
            Controller.logToAppLog(I18n.format("dlc.stop"));
            stage.fireEvent(new WindowEvent(
                stage,
                WindowEvent.WINDOW_CLOSE_REQUEST
           ));
        }
    }

    /**
     * Handle click at load config menu items.
     * Opens a file chooser dialog and loads a download config from a XML file.
     *
     * @param event
     *     The Event.
     */
    @FXML
    protected void handleLoadConfig(ActionEvent event) {
        try {
            File configFile = openConfigFileOpenDialog();
            loadConfigFromFile(configFile);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return;
        }
    }

    /**
     * Opens up a file dialog to choose a config file to load.
     *
     * @return The chosen file
     */
    protected File openConfigFileOpenDialog() {
        FileChooser fileChooser = new FileChooser();
        File initialDir = new File(
            Config.getInstance().getServices().getBaseDirectory().isEmpty()
            ? System.getProperty(USER_DIR)
            : Config.getInstance().getServices().getBaseDirectory());
        fileChooser.setInitialDirectory(initialDir);
        fileChooser.setTitle(I18n.getMsg("menu.load_config"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML Files", "*.xml"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fileChooser.showOpenDialog(menuBar.getScene().getWindow());
    }

    private void displayHTMLFileAsPopup(String popuptitle, String pathToFile)
        throws
        IOException {
        WebView web = new WebView();
        InputStream htmlPage = Misc.getResource(pathToFile);
        String content = IOUtils.toString(htmlPage, "UTF-8");
        web.getEngine().loadContent(content);
        WebViewWindow wvw = new WebViewWindow(web, popuptitle);
        wvw.popup();
    }

    /**
     * Loads config from a given file object.
     *
     * @param configFile
     *     File object holding the config file.
     */
    public void loadConfigFromFile(File configFile) {
        if (configFile == null) {
            return;
        }
        controller.resetGui();
        try {
            controller.downloadConfig = new DownloadConfig(configFile);
            String serviceURL = controller.downloadConfig.getServiceURL();
            serviceSelectionController.loadDownloadConfig(serviceURL, controller.downloadConfig);
        } catch (IOException
            | ParserConfigurationException
            | SAXException e) {
            LOG.error(e.getMessage(), e);
            statusLogController.setStatusTextUI(
                I18n.format("status.config.invalid-xml"));
            return;
        } catch (DownloadConfig.NoServiceURLException urlEx) {
            statusLogController.setStatusTextUI(
                I18n.format("status.config.no-url-provided"));
        }
    }

    private void openLinkFromFile(String pathToFile)
        throws IOException {
        InputStream is = Misc.getResource(pathToFile);
        String contents = IOUtils.toString(is, "UTF-8");
        if (contents == null
             || contents.isEmpty()
             || contents.equals("null")) {
            throw new MalformedURLException("URL is Empty");
        }
        URL helpURL = new URL(contents);
        Misc.startExternalBrowser(helpURL.toString());
    }

}
