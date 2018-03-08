package net.pan.textend;

import java.io.InputStream;
import java.io.IOException;

import java.time.LocalDate;

import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.application.Application;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javafx.scene.Node;
import javafx.scene.Scene;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class Main
extends Application
{
    private static final Logger logger =
        Logger.getLogger(Main.class.getName());

    private Stage stage;

    private TabPane tabPane;

    private ConnectionManager connectionManager;

    private URIDisplayer uriDisplayer;

    private String confirmExitTitle;
    private String confirmExitMessage;

    private static LocalDate buildDate;

    private static LocalDate getBuildDate()
    {
        if (buildDate == null)
        {
            Properties buildProperties = new Properties();
            try (InputStream stream =
                Main.class.getResource("build.properties").openStream())
            {
                buildProperties.load(stream);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Cannot load build.properties", e);
            }
            buildDate = LocalDate.of(
                Integer.parseInt(buildProperties.getProperty("build.year")),
                Integer.parseInt(buildProperties.getProperty("build.month")),
                Integer.parseInt(buildProperties.getProperty("build.day")));
        }
        return buildDate;
    }

    private static ResourceBundle resources()
    {
        return ResourceBundle.getBundle(
            Main.class.getPackage().getName() + ".Localization");
    }

    @Override
    public void init()
    {
        LoggerInitializer.parseLogLevelOptions(getParameters());
    }

    @Override
    public void start(Stage stage)
    {
        logger.info(() -> "Version " + version());

        this.stage = stage;

        uriDisplayer = new URIDisplayer(this);

        ResourceBundle res = resources();

        confirmExitTitle = res.getString("confirmExit.title");
        confirmExitMessage = res.getString("confirmExit.message");

        tabPane = new TabPane();

        //ConnectionInfo dummyInfo = new ConnectionInfo("localhost", 23);
        ConnectionInfo dummyInfo = new ConnectionInfo();
        Chat dummyChat = new Chat(dummyInfo, uriDisplayer);
        TabPane sizer = new TabPane(dummyChat.getTab());
        sizer.setVisible(false);

        StackPane tabContainer = new StackPane(tabPane, sizer);

        MenuItem openItem = new MenuItem(res.getString("open"));
        MenuItem closeItem = new MenuItem(res.getString("close"));
        MenuItem exitItem = new MenuItem(res.getString("exit"));

        MenuItem cutItem = new MenuItem(res.getString("cut"));
        MenuItem copyItem = new MenuItem(res.getString("copy"));
        MenuItem pasteItem = new MenuItem(res.getString("paste"));
        MenuItem prevItem = new MenuItem(res.getString("previous"));
        MenuItem nextItem = new MenuItem(res.getString("next"));

        MenuItem aboutItem = new MenuItem(res.getString("about"));

        openItem.setAccelerator(KeyCombination.valueOf(
            res.getString("open.accelerator")));
        closeItem.setAccelerator(KeyCombination.valueOf(
            res.getString("close.accelerator")));
        exitItem.setAccelerator(KeyCombination.valueOf(
            res.getString("exit.accelerator")));

        cutItem.setAccelerator(KeyCombination.valueOf(
            res.getString("cut.accelerator")));
        copyItem.setAccelerator(KeyCombination.valueOf(
            res.getString("copy.accelerator")));
        pasteItem.setAccelerator(KeyCombination.valueOf(
            res.getString("paste.accelerator")));
        nextItem.setAccelerator(KeyCombination.valueOf(
            res.getString("next.accelerator")));
        prevItem.setAccelerator(KeyCombination.valueOf(
            res.getString("previous.accelerator")));

        openItem.setOnAction(e -> showConnectionDialog());
        closeItem.setOnAction(e -> closeConnection());
        exitItem.setOnAction(e -> confirmExit(null));

        cutItem.setOnAction(e -> doTextAction(TextInputControl::cut));
        copyItem.setOnAction(e -> copy());
        pasteItem.setOnAction(e -> doTextAction(TextInputControl::paste));

        prevItem.setOnAction(e-> 
            currentChat().ifPresent(c -> c.previousInHistory()));
        nextItem.setOnAction(e-> 
            currentChat().ifPresent(c -> c.nextInHistory()));

        aboutItem.setOnAction(e -> showAbout());

        MenuBar menuBar = new MenuBar(
            new Menu(res.getString("menu.connection"), null,
                openItem, closeItem, exitItem),
            new Menu(res.getString("menu.edit"), null,
                cutItem, copyItem, pasteItem,
                /*new SeparatorMenuItem(), undoItem, redoItem,*/
                new SeparatorMenuItem(), prevItem, nextItem),
            new Menu(res.getString("menu.help"), null,
                aboutItem));
        menuBar.setUseSystemMenuBar(true);

        BorderPane contents = new BorderPane(
            tabContainer, menuBar, null, null, null);

        stage.setTitle(res.getString("title.default"));
        stage.getIcons().setAll(
            Arrays.stream(res.getString("icons").split("\\s+"))
                .map(url -> new Image(Main.class.getResource(url).toString()))
                .collect(Collectors.toList()));
        stage.setScene(new Scene(contents));
        stage.show();

        stage.setOnCloseRequest(this::confirmExit);

        ReadOnlyObjectProperty<Node> focusOwner =
            stage.getScene().focusOwnerProperty();
        ReadOnlyObjectProperty<Tab> selectedTab =
            tabPane.getSelectionModel().selectedItemProperty();

        BooleanBinding isTextControl = Bindings.createBooleanBinding(
            () -> focusOwner.get() instanceof TextInputControl, focusOwner);
        BooleanBinding cannotChangeTextControl =
            isTextControl.not()
                .or(Bindings.selectBoolean(focusOwner, "disabled"))
                .or(Bindings.selectBoolean(focusOwner, "editable").not());

        pasteItem.disableProperty().bind(cannotChangeTextControl);
        cutItem.disableProperty().bind(cannotChangeTextControl.or(
            Bindings.selectString(focusOwner, "selectedText").isEmpty()));
        copyItem.disableProperty().bind(cannotChangeTextControl.and(
            Bindings.selectBoolean(selectedTab, "userData", "focused").and(
            Bindings.selectBoolean(selectedTab, "userData", "copyable")).not()));

        BooleanBinding notConnected =
            Bindings.selectBoolean(selectedTab, "userData", "connected").not();
        prevItem.disableProperty().bind(notConnected);
        nextItem.disableProperty().bind(notConnected);

        showConnectionDialog();
    }

    private void confirmExit(WindowEvent event)
    {
        if (tabPane.getTabs().isEmpty())
        {
            // No need for a confirm dialog if there's no connections.
            exit();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            confirmExitMessage, ButtonType.YES, ButtonType.NO);
        alert.initOwner(stage);
        alert.setTitle(confirmExitTitle);
        //alert.setHeader(null);

        Optional<ButtonType> response = alert.showAndWait();
        if (response.filter(r -> r == ButtonType.YES).isPresent())
        {
            exit();
        }
        else if (event != null)
        {
            event.consume();
        }
    }

    private void exit()
    {
        System.exit(0);
    }

    private void showConnectionDialog()
    {
        if (connectionManager == null)
        {
            connectionManager = new ConnectionManager(stage);
        }

        ConnectionInfo connectionInfo = connectionManager.show();
        logger.info(() -> "Connecting to " + connectionInfo);

        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8140491
        stage.hide();
        stage.show();

        if (connectionInfo != null)
        {
            connectTo(connectionInfo);
        }
    }

    private void connectTo(ConnectionInfo connectionInfo)
    {
        Chat chat = new Chat(connectionInfo, uriDisplayer);
        Tab tab = chat.getTab();
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        chat.connect();
    }

    private void closeConnection()
    {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        Chat chat = (Chat) tab.getUserData();
        assert chat != null : "Tab should have a Chat instance as its UserData";

        chat.close();
    }

    private boolean inMutableTextField()
    {
        Node node = stage.getScene().getFocusOwner();
        if (node instanceof TextInputControl && !node.isDisabled())
        {
            return ((TextInputControl) node).isEditable();
        }
        return false;
    }

    private boolean cannotCopy()
    {
        Node focusOwner = stage.getScene().getFocusOwner();
        return !(focusOwner instanceof TextInputControl);
    }

    private void doTextAction(Consumer<TextInputControl> action)
    {
        Node focusOwner = stage.getScene().getFocusOwner();
        if (focusOwner instanceof TextInputControl)
        {
            action.accept((TextInputControl) focusOwner);
        }
    }

    private Optional<Chat> currentChat()
    {
        return Optional.of(tabPane.getSelectionModel().getSelectedItem()).map(
            tab -> (Chat) tab.getUserData());
    }

    private boolean currentChatContains(Node node)
    {
        return currentChat().filter(c -> c.contains(node)).isPresent();
    }

    private void copy()
    {
        Node focusOwner = stage.getScene().getFocusOwner();
        if (focusOwner instanceof TextInputControl)
        {
            ((TextInputControl) focusOwner).copy();
        }
        else if (currentChatContains(focusOwner))
        {
            currentChat().get().copy();
        }
    }

    private static String version()
    {
        String version = Main.class.getPackage().getImplementationVersion();
        if (version == null)
        {
            Module module = Main.class.getModule();
            Optional<?> moduleVersion = module.getDescriptor().version();
            version = moduleVersion.map(Object::toString).orElse("(unknown)");
        }
        return version;
    }

    private void showAbout()
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.getDialogPane().getStylesheets().add(
            Main.class.getResource("style.css").toString());
        alert.getDialogPane().getStyleClass().add("about");

        ResourceBundle res = resources();
        alert.setTitle(res.getString("about.title"));

        ImageView image = new ImageView(
            Main.class.getResource(res.getString("about.icon")).toString());
        String name = res.getString("about.header");
        String details = String.format(res.getString("about.text"),
            version(), getBuildDate());

        alert.setHeaderText(null);
        alert.setGraphic(image);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("name");

        alert.getDialogPane().setContent(
            new VBox(24, nameLabel, new Label(details)));

        alert.show();
    }
}
