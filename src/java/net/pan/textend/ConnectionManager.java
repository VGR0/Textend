package net.pan.textend;

// TODO: limit count field to positive numbers
// TODO: validate datetime pattern

import java.text.MessageFormat;
import java.text.ParsePosition;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.EnumMap;

import java.util.Formatter;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.w3c.dom.Element;

import javafx.stage.Window;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

//import javafx.scene.control.Spinner;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;

import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
//import javafx.beans.binding.IntegerExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.stage.FileChooser;

import static net.pan.textend.Action.Type.COLOR;
import static net.pan.textend.Action.Type.TIMESTAMP;
import static net.pan.textend.Action.Type.SEND;
import static net.pan.textend.Action.Type.SOUND;

/**
 * User interface for add, removing, and editing {@link ConnectionInfo} data.
 */
class ConnectionManager
{
    private static final Logger logger =
        Logger.getLogger(ConnectionManager.class.getName());

    /**
     * Whether to return the selected {@code ConnectionInfo} when returning
     * from {@code show()}.
     */
    private boolean connectToSelection;

    private final ConnectionInfoPersistor persistor;

    private final Dialog<ButtonType> dialog;

    private final Pagination paginator;
    //private final StackPane pagesPane;

    //private final ListView<ConnectionInfo> connectionList;
    private final TableView<ConnectionInfo> connectionList;

    // Connection fields

    private final TextField nameField;

    private final TextField hostField;

    // Currently, Spinner does not allow a value to be typed in,
    // so it's unsuitable for port number entry.
    //private final Spinner<Integer> portField;
    private final TextField portField;

    private final TextFormatter<Integer> portFormatter;

    private final TextField userField;

    private final PasswordField passwordField;

    private final PatternField userPromptPatternField;

    private final RadioButton passwordPromptButton;

    private final PatternField passwordPromptPatternField;

    private final TextField passwordSeparatorField;

    private final CheckBox sslField;

    private final CheckBox validCertsField;

    private final CheckBox useSystemColorsField;

    private final ColorPicker foregroundField;

    private final ColorPicker backgroundField;

    private final SampleField colorSampleField;

    private final ComboBox<TriggerType> triggerTypeList;

    //private final ListView<Trigger> triggerList;
    private final TableView<Trigger> triggerList;

    // Trigger fields

    private final TextField triggerNameField;

    private final PatternField triggerPatternField;

    private final CheckBox oneTimeField;

    private final TextArea commentsField;

    private final TableView<Action> actionList;

    private final String sendActionLineSeparator;

    // Trigger action fields

    private final TextArea textToSendField;

    private final TextField soundFileField;

    private final FileChooser soundFileChooser;

    private final ComboBox<FormatType> timeStyleField;

    private final ComboBox<FormatType> dateStyleField;

    private final TextField dateTimeFormatField;

    private final CheckBox actionOverrideForegroundField;

    private final CheckBox actionOverrideBackgroundField;

    private final ColorPicker actionForegroundField;

    private final ColorPicker actionBackgroundField;

    // Other data

    private final MessageFormat defaultConnectionNameFormat;

    private final MessageFormat defaultTriggerNameFormat;

    private final String deleteConnectionConfirmationTitle;
    private final String deleteConnectionConfirmationHeader;
    private final MessageFormat deleteConnectionConfirmationMessageFormat;

    private final String deleteTriggerConfirmationTitle;
    private final String deleteTriggerConfirmationHeader;
    private final MessageFormat deleteTriggerConfirmationMessageFormat;

    private final String deleteActionConfirmationTitle;
    private final String deleteActionConfirmationHeader;
    private final MessageFormat deleteActionConfirmationMessageFormat;

    private final String duplicateTriggerTitle;
    private final String duplicateTriggerHeader;

    private final ErrorHandler loadFailedHandler;

    /**
     * Arguments: name
     */
    private final MessageFormat duplicateTriggerMessageFormat;

    private final String invalidSoundTitle;
    /**
     * Arguments: file count
     */
    private final MessageFormat invalidSoundHeaderFormat;
    /**
     * Arguments: file count
     */
    private final MessageFormat invalidSoundMessageFormat;

    private final List<Region> pages;

    private final List<Node> initialFocus;

    ConnectionManager(Window parent)
    {
        persistor = new ConnectionInfoPersistor();
        persistor.setOnFailed(e -> showLoadFailure());
        // TODO: error handling, and maybe pseudo-modal "loading" message

        ResourceBundle res = ResourceBundle.getBundle(
            ConnectionManager.class.getPackage().getName() + ".Localization");

        defaultConnectionNameFormat =
            new MessageFormat(res.getString("connection.defaultNameFormat"));
        defaultTriggerNameFormat =
            new MessageFormat(res.getString("trigger.defaultNameFormat"));

        deleteConnectionConfirmationTitle =
            res.getString("connection.delete.confirm.title");
        deleteConnectionConfirmationHeader =
            res.getString("connection.delete.confirm.header");
        deleteConnectionConfirmationMessageFormat = new MessageFormat(
            res.getString("connection.delete.confirm.message"));

        deleteTriggerConfirmationTitle =
            res.getString("trigger.delete.confirm.title");
        deleteTriggerConfirmationHeader =
            res.getString("trigger.delete.confirm.header");
        deleteTriggerConfirmationMessageFormat = new MessageFormat(
            res.getString("trigger.delete.confirm.message"));

        deleteActionConfirmationTitle =
            res.getString("actions.delete.confirm.title");
        deleteActionConfirmationHeader =
            res.getString("actions.delete.confirm.header");
        deleteActionConfirmationMessageFormat = new MessageFormat(
            res.getString("actions.delete.confirm.message"));

        MessageFormat loadFailedMessageFormat = new MessageFormat(
            res.getString("connections.loadFailure.message"));
        loadFailedHandler = new ErrorHandler(
            res.getString("connections.loadFailure.title"),
            res.getString("connections.loadFailure.header"),
            t -> loadFailedMessageFormat.format(new Object[] { t }));

        duplicateTriggerMessageFormat = new MessageFormat(
            res.getString("connections.trigger.duplicate.message"));
        duplicateTriggerTitle =
            res.getString("connections.trigger.duplicate.title");
        duplicateTriggerHeader =
            res.getString("connections.trigger.duplicate.header");

        invalidSoundTitle =
            res.getString("actions.invalidSound.title");
        invalidSoundHeaderFormat = new MessageFormat(
            res.getString("actions.invalidSound.header"));
        invalidSoundMessageFormat = new MessageFormat(
            res.getString("actions.invalidSound.message"));

        ScrollBar scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);
        ReadOnlyDoubleProperty scrollBarWidth = scrollBar.widthProperty();

        connectionList = new TableView<>();
        connectionList.setPlaceholder(
            createTablePlaceholder(res.getString("connection.empty")));
        TableColumn<ConnectionInfo, String> nameCol =
            new TableColumn<>(res.getString("connection.column.name"));
        nameCol.setCellValueFactory(f -> f.getValue().nameProperty());
        nameCol.prefWidthProperty().bind(
            connectionList.widthProperty().subtract(scrollBarWidth));
        connectionList.getColumns().add(nameCol);
        Label nameColSizer = new Label(defaultNewConnectionName());
        connectionList.itemsProperty().bind(persistor.valueProperty());
        connectionList.setOnMouseClicked(e -> edit(e, this::connect));
        connectionList.setOnKeyPressed(e -> edit(e, this::connect));

        connectionList.itemsProperty().addListener(
            (obs, old, items) -> selectFirst(connectionList));
        connectionList.sceneProperty().addListener((obs, old, scene) -> 
            Platform.runLater(() -> Platform.runLater(
                () -> connectionList.requestFocus())));

        Button backToConnectionsButton = createButton(
            res.getString("trigger.backToConnections"),
            this::hideConnectionFields);
        Button backToTriggersButton = createButton(
            res.getString("action.backToTriggers"),
            this::hideTriggerFields);

        ReadOnlyObjectProperty<ConnectionInfo> selectedConnection =
            connectionList.getSelectionModel().selectedItemProperty();

        nameField = new TextField();
        nameField.setPrefColumnCount(12);
        nameField.textProperty().bindBidirectional(
            new ChainedStringProperty<ConnectionInfo>(
                selectedConnection, ConnectionInfo::nameProperty));

        hostField = new TextField();
        hostField.setPrefColumnCount(12);
        hostField.textProperty().bindBidirectional(
            new ChainedStringProperty<ConnectionInfo>(
                selectedConnection, ConnectionInfo::hostProperty));

        //portField = new Spinner<>(1, 65535, 23);
        portField = new TextField();
        portField.setPrefColumnCount(6);
        portFormatter = new TextFormatter<>(new PortConverter());
            //new PortConverter(), 23, this::restrictPortChars);
        portField.setTextFormatter(portFormatter);
        portFormatter.valueProperty().bindBidirectional(
            new ChainedObjectProperty<Integer, ConnectionInfo>(
                selectedConnection, c -> c.portProperty().asObject()));

        userField = new TextField();
        userField.setPrefColumnCount(12);
        userField.textProperty().bindBidirectional(
            new ChainedStringProperty<ConnectionInfo>(
                selectedConnection, ConnectionInfo::userProperty));

        passwordField = new PasswordField();
        passwordField.setPrefColumnCount(12);
        passwordField.textProperty().bindBidirectional(
            new ChainedStringProperty<ConnectionInfo>(
                selectedConnection, ConnectionInfo::passwordProperty));

        userPromptPatternField =
            new PatternField(res.getString("connection.user.change"));

        ObjectProperty<Trigger> userTrigger =
            new ChainedObjectProperty<Trigger, ConnectionInfo>(
                selectedConnection, ConnectionInfo::userTriggerProperty);
        userPromptPatternField.patternProperty().bindBidirectional(
            new ChainedStringProperty<Trigger>(
                userTrigger, Trigger::patternProperty));
        userPromptPatternField.typeProperty().bindBidirectional(
            new ChainedObjectProperty<PatternType, Trigger>(
                userTrigger, Trigger::patternTypeProperty));

        passwordPromptPatternField =
            new PatternField(res.getString("connection.password.change"));

        ObjectProperty<Trigger> passwordTrigger =
            new ChainedObjectProperty<Trigger, ConnectionInfo>(
                selectedConnection, ConnectionInfo::passwordTriggerProperty);
        passwordPromptPatternField.patternProperty().bindBidirectional(
            new ChainedStringProperty<Trigger>(
                passwordTrigger, Trigger::patternProperty));
        passwordPromptPatternField.typeProperty().bindBidirectional(
            new ChainedObjectProperty<PatternType, Trigger>(
                passwordTrigger, Trigger::patternTypeProperty));

        passwordSeparatorField = new TextField();
        passwordSeparatorField.setPrefColumnCount(8);

        passwordPromptButton =
            createRadioButton(res.getString("connection.password.prompt"));

        RadioButton passwordAfterUserButton =
            createRadioButton(res.getString("connection.password.afterUser"));
        passwordAfterUserButton.selectedProperty().bindBidirectional(
            new ChainedBooleanProperty<ConnectionInfo>(
                selectedConnection,
                ConnectionInfo::sendPasswordWithUserProperty));
        passwordPromptButton.selectedProperty().bind(
            passwordAfterUserButton.selectedProperty().not());

        ToggleGroup passwordTriggerGroup = new ToggleGroup();
        passwordTriggerGroup.getToggles().addAll(
            passwordPromptButton, passwordAfterUserButton);

        passwordPromptPatternField.node().disableProperty().bind(
            passwordPromptButton.selectedProperty().not());
        passwordSeparatorField.disableProperty().bind(
            passwordAfterUserButton.selectedProperty().not());

        sslField = new CheckBox(res.getString("connection.ssl"));
        sslField.selectedProperty().bindBidirectional(
            new ChainedBooleanProperty<ConnectionInfo>(
                selectedConnection,
                ConnectionInfo::SSLProperty));

        validCertsField = new CheckBox(res.getString("connection.validCerts"));
        validCertsField.selectedProperty().bindBidirectional(
            new ChainedBooleanProperty<ConnectionInfo>(
                selectedConnection,
                ConnectionInfo::requireValidCertificateProperty));

        validCertsField.disableProperty().bind(
            sslField.selectedProperty().not());

        useSystemColorsField = new CheckBox(
            res.getString("connection.useSystemColors"));
        useSystemColorsField.selectedProperty().bindBidirectional(
            new ChainedBooleanProperty<ConnectionInfo>(
                selectedConnection,
                ConnectionInfo::useSystemColorsProperty));

        foregroundField = new ColorPicker();
        foregroundField.valueProperty().bindBidirectional(
            new ChainedObjectProperty<Color, ConnectionInfo>(
                selectedConnection,
                ConnectionInfo::foregroundProperty));
        foregroundField.disableProperty().bind(
            useSystemColorsField.selectedProperty());

        backgroundField = new ColorPicker();
        backgroundField.valueProperty().bindBidirectional(
            new ChainedObjectProperty<Color, ConnectionInfo>(
                selectedConnection,
                ConnectionInfo::backgroundProperty));
        backgroundField.disableProperty().bind(
            useSystemColorsField.selectedProperty());

        String sampleText = res.getString("connection.colorSample");

        Label colorSampleLabel = new Label(sampleText);
        colorSampleLabel.getStyleClass().add("color-sample");
        colorSampleLabel.setVisible(false);

        colorSampleField = new SampleField();
        WebView colorSampleView = colorSampleField.view;
        colorSampleView.getEngine().loadContent(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\""
                               + " \"http://www.w3.org/TR/html4/strict.dtd\">"
            + "<html><body id='body'>"
            + "<pre id='content' style='white-space: pre-wrap;'>"
            + "<span id='text'>" + escapeHTML(sampleText) + "</span>"
            + "</pre></body></html>");
        colorSampleField.bindSizeTo(colorSampleLabel);

        useSystemColorsField.selectedProperty().addListener(
            (obs, old, use) -> updateColors());
        foregroundField.valueProperty().addListener(
            (obs, old, color) -> updateColors());
        backgroundField.valueProperty().addListener(
            (obs, old, color) -> updateColors());

        StackPane colorSample =
            new StackPane(colorSampleLabel, colorSampleField.getNode());

        triggerTypeList = EnumListCell.createEnumList(TriggerType.class, res);

        triggerList = new TableView<>();
        triggerList.setPlaceholder(
            createTablePlaceholder(res.getString("trigger.empty")));
        TableColumn<Trigger, String> triggerNameCol =
            new TableColumn<>(res.getString("connection.trigger.column.name"));
        triggerNameCol.setCellValueFactory(f -> f.getValue().nameProperty());
        triggerNameCol.setSortable(false);
        triggerNameCol.prefWidthProperty().bind(
            triggerList.widthProperty().subtract(scrollBarWidth));
        triggerList.getColumns().add(triggerNameCol);

        triggerList.setOnMouseClicked(e -> edit(e, this::editTrigger));

        TextField rowSizer = new TextField();
        triggerList.prefHeightProperty().bind(
            rowSizer.prefHeightProperty().multiply(6));

        selectedConnection.addListener(
            (obs, old, connection) -> updateTriggerList(connection));

        ReadOnlyObjectProperty<Trigger> selectedTrigger =
            triggerList.getSelectionModel().selectedItemProperty();

        triggerNameField = new TextField();
        triggerNameField.setPrefColumnCount(12/*24*/);
        triggerNameField.textProperty().bindBidirectional(
            new ChainedStringProperty<Trigger>(
                selectedTrigger, Trigger::nameProperty));

        triggerPatternField = new PatternField(
            res.getString("connection.trigger.pattern.change"));
        triggerPatternField.patternProperty().bindBidirectional(
            new ChainedStringProperty<Trigger>(
                selectedTrigger, Trigger::patternProperty));
        triggerPatternField.typeProperty().bindBidirectional(
            new ChainedObjectProperty<PatternType, Trigger>(
                selectedTrigger, Trigger::patternTypeProperty));

        oneTimeField = new CheckBox(
            res.getString("connection.trigger.oneTime"));
        oneTimeField.selectedProperty().bindBidirectional(
            new ChainedBooleanProperty<Trigger>(
                selectedTrigger, Trigger::oneTimeProperty));

        commentsField = new TextArea();
        commentsField.setWrapText(true);
        commentsField.setPrefColumnCount(20);
        commentsField.setPrefRowCount(3);
        commentsField.textProperty().bindBidirectional(
            new ChainedStringProperty<Trigger>(
                selectedTrigger, Trigger::commentsProperty));

        actionList = new TableView<>();
        actionList.setPlaceholder(
            createTablePlaceholder(res.getString("actions.empty")));
        actionList.itemsProperty().bindBidirectional(
            new ChainedObjectProperty<ObservableList<Action>, Trigger>(
                selectedTrigger, Trigger::actionsProperty));
        actionList.prefHeightProperty().bind(
            rowSizer.prefHeightProperty().multiply(4));

        ReadOnlyObjectProperty<Action> selectedAction =
            actionList.getSelectionModel().selectedItemProperty();

        ObservableMap<Action.Type, String> actionTypes =
            FXCollections.observableMap(new EnumMap<>(Action.Type.class));
        for (Action.Type type : Action.Type.values())
        {
            actionTypes.put(type, res.getString("actions.type." + type));
        }

        sendActionLineSeparator =
            res.getString("actions.summary.send.separator");

        TableColumn<Action, String> actionTypeColumn =
            new TableColumn<>(res.getString("actions.column.type"));
        actionTypeColumn.setCellValueFactory(f -> Bindings.stringValueAt(
            actionTypes, f.getValue().typeProperty()));
        actionTypeColumn.setSortable(false);

        TableColumn<Action, String> actionDescColumn =
            new TableColumn<>(res.getString("actions.column.desc"));
        actionDescColumn.setCellValueFactory(
            f -> f.getValue().summaryProperty());
        actionDescColumn.setSortable(false);
        // Make this column's width the remainder of the table's width.
        actionDescColumn.prefWidthProperty().bind(
            actionList.widthProperty()
                .subtract(actionTypeColumn.widthProperty()).subtract(5)
                .subtract(scrollBarWidth));

        actionList.getColumns().add(actionTypeColumn);
        actionList.getColumns().add(actionDescColumn);

        textToSendField = new TextArea();
        textToSendField.setPrefColumnCount(20);
        textToSendField.setPrefRowCount(3);
        textToSendField.textProperty().bindBidirectional(
            new ChainedStringProperty<Action>(
                selectedAction, Action::detailProperty));

        soundFileField = new TextField();
        soundFileField.setPrefColumnCount(12);
        soundFileField.textProperty().bindBidirectional(
            new ChainedStringProperty<Action>(
                selectedAction, Action::detailProperty));

        soundFileChooser = new FileChooser();
        soundFileChooser.setTitle(res.getString("actions.sound.chooser.title"));
        soundFileChooser.initialFileNameProperty().bind(
            soundFileField.textProperty());
        soundFileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(
                res.getString("actions.sound.chooser.filter"),
                "*.aac", "*.aiff", "*.mp3", "*.pcm", "*.wav"),
            new FileChooser.ExtensionFilter(
                res.getString("actions.sound.chooser.filter.all"),
                "*"));

        timeStyleField = EnumListCell.createEnumList(FormatType.class, res,
            "timestamp.time.formatType.");
        dateStyleField = EnumListCell.createEnumList(FormatType.class, res,
            "timestamp.date.formatType.");

        // Move FormatType.NONE to the end of the item list.
        timeStyleField.getItems().remove(FormatType.NONE);
        timeStyleField.getItems().add(FormatType.NONE);

        timeStyleField.valueProperty().bindBidirectional(
            new ChainedObjectProperty<FormatType, Action>(
                selectedAction, Action::timeStyleProperty));
        dateStyleField.valueProperty().bindBidirectional(
            new ChainedObjectProperty<FormatType, Action>(
                selectedAction, Action::dateStyleProperty));

        dateTimeFormatField = new TextField();
        dateTimeFormatField.setPrefColumnCount(8);
        dateTimeFormatField.textProperty().bindBidirectional(
            new ChainedStringProperty<Action>(
                selectedAction, Action::detailProperty));

        actionOverrideForegroundField = new CheckBox(
            res.getString(
                "connection.trigger.action.colors.foreground.specified"));
        actionOverrideForegroundField.selectedProperty().bindBidirectional(
            new ChainedBooleanProperty<Action>(
                selectedAction,
                Action::foregroundSpecifiedProperty));

        actionOverrideBackgroundField = new CheckBox(
            res.getString(
                "connection.trigger.action.colors.background.specified"));
        actionOverrideBackgroundField.selectedProperty().bindBidirectional(
            new ChainedBooleanProperty<Action>(
                selectedAction,
                Action::backgroundSpecifiedProperty));

        actionForegroundField = new ColorPicker();
        actionForegroundField.valueProperty().bindBidirectional(
            new ChainedObjectProperty<Color, Action>(
                selectedAction, Action::foregroundProperty));
        actionForegroundField.disableProperty().bind(
            actionOverrideForegroundField.selectedProperty().not());

        actionBackgroundField = new ColorPicker();
        actionBackgroundField.valueProperty().bindBidirectional(
            new ChainedObjectProperty<Color, Action>(
                selectedAction, Action::backgroundProperty));
        actionBackgroundField.disableProperty().bind(
            actionOverrideBackgroundField.selectedProperty().not());

        BooleanExpression isReceive =
            triggerTypeList.valueProperty().isEqualTo(TriggerType.RECEIVE);

        Map<Action.Type, RadioButton> actionTypeButtons =
            new EnumMap<>(Action.Type.class);
        ToggleGroup actionTypeGroup = new ToggleGroup();
        for (Action.Type type : Action.Type.values())
        {
            RadioButton button = createRadioButton(
                res.getString("action.type." + type));
            button.setUserData(type);
            actionTypeButtons.put(type, button);
            actionTypeGroup.getToggles().add(button);

/*
            if (type.category == Action.Category.MODIFICATION)
            {
                button.visibleProperty().bind(isReceive);
                button.managedProperty().bind(isReceive);
            }
*/
        }

        VBox actionTypesPane = new VBox(6);
        for (Action.Category category : Action.Category.values())
        {
            Node heading = 
                new Text(res.getString("action.type.heading." + category));
            Node buttonsPane = createButtonPane(
                actionTypeButtons.entrySet().stream()
                    .filter(e -> e.getKey().category == category)
                    .map(e -> e.getValue())
                    .toArray(Node[]::new));
            Node categoryPane =
                new BorderPane(buttonsPane, heading, null, null, null);
            BorderPane.setMargin(buttonsPane, new Insets(6, 0, 0, 12));

            if (category == Action.Category.MODIFICATION)
            {
                categoryPane.visibleProperty().bind(isReceive);
                categoryPane.managedProperty().bind(isReceive);
            }

            actionTypesPane.getChildren().add(categoryPane);
        }

        BooleanExpression showColor =
            triggerTypeList.valueProperty().isEqualTo(TriggerType.RECEIVE);
        actionTypeButtons.get(COLOR).visibleProperty().bind(showColor);
        actionTypeButtons.get(COLOR).managedProperty().bind(showColor);

        actionTypeGroup.selectToggle(actionTypeButtons.get(SEND));

        // Bind action type radio buttons bidirectionally to selectedAction.type
        actionTypeGroup.selectedToggleProperty().addListener(
            (obs, old, button) -> updateAction(selectedAction.get(), button));
        ObjectProperty<Action.Type> typeProperty =
            new ChainedObjectProperty<Action.Type, Action>(
                selectedAction, Action::typeProperty);
        typeProperty.addListener((obs, old, type) ->
            actionTypeGroup.selectToggle(actionTypeButtons.get(type)));

        Button createConnectionButton = createButton(
            res.getString("connection.create"), this::createConnection);
        Button connectButton = createButton(
            res.getString("connection.connect"), this::connect);
        Button editConnectionButton = createButton(
            res.getString("connection.edit"), this::editConnection);
        Button removeConnectionButton = createButton(
            res.getString("connection.delete"), this::deleteConnection);

        Button soundFileChooseButton = createButton(
            res.getString("actions.sound.choose"), this::showSoundFileChooser);

        Button createTriggerButton = createButton(
            res.getString("trigger.create"), this::createTrigger);
        Button editTriggerButton = createButton(
            res.getString("trigger.edit"), this::editTrigger);
        Button moveTriggerUpButton = createButton(
            res.getString("trigger.up"), this::moveTriggerUp);
        Button moveTriggerDownButton = createButton(
            res.getString("trigger.down"), this::moveTriggerDown);
        Button moveTriggerToTopButton = createButton(
            res.getString("trigger.toTop"), this::moveTriggerToTop);
        Button moveTriggerToBottomButton = createButton(
            res.getString("trigger.toBottom"), this::moveTriggerToBottom);
        Button removeTriggerButton = createButton(
            res.getString("trigger.delete"), this::deleteTrigger);

        Button createActionButton = createButton(
            res.getString("actions.create"), this::createAction);
        Button moveActionUpButton = createButton(
            res.getString("actions.up"), this::moveActionUp);
        Button moveActionDownButton = createButton(
            res.getString("actions.down"), this::moveActionDown);
        Button removeActionButton = createButton(
            res.getString("actions.delete"), this::deleteAction);

        BooleanExpression noSingleConnectionSelected = Bindings.size(
            connectionList.getSelectionModel().getSelectedIndices())
            .isNotEqualTo(1);

        ObservableList<Integer> selectedTriggers =
            triggerList.getSelectionModel().getSelectedIndices();
        ObservableList<Integer> selectedActions =
            actionList.getSelectionModel().getSelectedIndices();

        connectButton.disableProperty().bind(noSingleConnectionSelected);
        editConnectionButton.disableProperty().bind(noSingleConnectionSelected);
        removeConnectionButton.disableProperty().bind(
            selectedConnection.isNull());
        moveTriggerUpButton.disableProperty().bind(
            cannotMoveUp(triggerList));
        moveTriggerDownButton.disableProperty().bind(
            cannotMoveDown(triggerList));
        moveTriggerToTopButton.disableProperty().bind(
            cannotMoveUp(triggerList));
        moveTriggerToBottomButton.disableProperty().bind(
            cannotMoveDown(triggerList));
        removeTriggerButton.disableProperty().bind(
            selectedTrigger.isNull());
        moveActionUpButton.disableProperty().bind(
            cannotMoveUp(actionList));
        moveActionDownButton.disableProperty().bind(
            cannotMoveDown(actionList));
        editTriggerButton.disableProperty().bind(Bindings.size(
            triggerList.getSelectionModel().getSelectedIndices())
            .isNotEqualTo(1));
        removeActionButton.disableProperty().bind(
            selectedAction.isNull());

        Label connectionListLabel = LabelFactory.createLabel(
            res.getString("connection.names"), connectionList);

        Label nameLabel = LabelFactory.createLabel(
            res.getString("connection.name"), nameField);
        Label hostLabel = LabelFactory.createLabel(
            res.getString("connection.host"), hostField);
        Label portLabel = LabelFactory.createLabel(
            res.getString("connection.port"), portField);
        Label userLabel = LabelFactory.createLabel(
            res.getString("connection.user"), userField);
        Label passwordLabel = LabelFactory.createLabel(
            res.getString("connection.password"), passwordField);

        Label foregroundLabel = LabelFactory.createLabel(
            res.getString("connection.foreground"), foregroundField);
        Label backgroundLabel = LabelFactory.createLabel(
            res.getString("connection.background"), backgroundField);

        Label userPromptLabel = new Label(
            res.getString("connection.user.prompt"));
        userPromptLabel.setMinWidth(Region.USE_PREF_SIZE);

        Label triggersLabel = LabelFactory.createLabel(
            res.getString("connection.triggers"), triggerList);
        Label triggerTypeLabel = LabelFactory.createLabel(
            res.getString("connection.triggers.type"), triggerTypeList);
        Label triggerNameLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.name"), triggerNameField);
        Label triggerPatternLabel = new Label(
            res.getString("connection.trigger.pattern"));
        Label commentsLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.comments"), commentsField);

        Label actionsLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.actions"), actionList);
        Label textToSendLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.action.textToSend"),
            textToSendField);
        Label soundFileLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.action.soundFile"),
            soundFileField);
        Label timeStyleLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.action.timestamp.timeStyle"),
            timeStyleField);
        Label dateStyleLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.action.timestamp.dateStyle"),
            dateStyleField);
        Label dateTimeFormatLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.action.timestamp.pattern"),
            dateTimeFormatField);
        Label actionForegroundLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.action.colors.foreground"),
            actionForegroundField);
        Label actionBackgroundLabel = LabelFactory.createLabel(
            res.getString("connection.trigger.action.colors.background"),
            actionBackgroundField);

        triggerPatternLabel.setMinWidth(Region.USE_PREF_SIZE);

        GridPane sendActionPane = new GridPane();
        sendActionPane.setVgap(3);
        sendActionPane.addColumn(0, textToSendLabel, textToSendField);
        GridPane.setHgrow(textToSendField, Priority.ALWAYS);
        GridPane.setVgrow(textToSendField, Priority.ALWAYS);

        GridPane soundActionPane = new GridPane();
        soundActionPane.setHgap(6);
        soundActionPane.setVgap(3);
        soundActionPane.addRow(0, soundFileLabel);
        soundActionPane.addRow(1, soundFileField, soundFileChooseButton);
        GridPane.setColumnSpan(soundFileLabel, GridPane.REMAINING);
        GridPane.setHgrow(soundFileField, Priority.ALWAYS);

        GridPane colorActionPane = new GridPane();
        colorActionPane.setHgap(6);
        colorActionPane.setVgap(6);
        int row = -1;
        colorActionPane.addRow(++row,
            actionForegroundLabel, createBaselineAlignedRow(
                actionOverrideForegroundField,
                actionForegroundField));
        colorActionPane.addRow(++row,
            actionBackgroundLabel, createBaselineAlignedRow(
                actionOverrideBackgroundField,
                actionBackgroundField));
        colorActionPane.addRow(++row, timeStyleLabel, timeStyleField);
        colorActionPane.addRow(++row, dateStyleLabel, dateStyleField);
        colorActionPane.addRow(++row, dateTimeFormatLabel, dateTimeFormatField);

        BooleanExpression isTimestamp =
            actionTypeButtons.get(TIMESTAMP).selectedProperty();

        colorActionPane.visibleProperty().bind(
            actionTypeButtons.get(COLOR).selectedProperty().or(isTimestamp));
        sendActionPane.visibleProperty().bind(
            actionTypeButtons.get(SEND).selectedProperty());
        soundActionPane.visibleProperty().bind(
            actionTypeButtons.get(SOUND).selectedProperty());

        timeStyleLabel.visibleProperty().bind(isTimestamp);
        timeStyleField.visibleProperty().bind(isTimestamp);
        dateStyleLabel.visibleProperty().bind(isTimestamp);
        dateStyleField.visibleProperty().bind(isTimestamp);
        dateStyleField.disableProperty().bind(
            timeStyleField.valueProperty().isEqualTo(FormatType.NONE));
        dateTimeFormatLabel.visibleProperty().bind(isTimestamp.and(
            timeStyleField.valueProperty().isEqualTo(FormatType.NONE)));
        dateTimeFormatField.visibleProperty().bind(isTimestamp.and(
            timeStyleField.valueProperty().isEqualTo(FormatType.NONE)));
/*
        for (Node timestampNode : Arrays.asList(
            timeStyleLabel, timeStyleField, dateStyleLabel, dateStyleField,
            dateTimeFormatLabel, dateTimeFormatField))
        {
            timestampNode.visibleProperty().bind(isTimestamp);
            timestampNode.managedProperty().bind(isTimestamp);
        }
*/

        StackPane actionDetailsPane =
            new StackPane(sendActionPane, soundActionPane, colorActionPane);

        GridPane triggerTextFieldsPane = new GridPane();
        triggerTextFieldsPane.setPadding(new Insets(6));
        triggerTextFieldsPane.setHgap(6);
        triggerTextFieldsPane.setVgap(6);
        row = -1;
        triggerTextFieldsPane.addRow(++row, backToTriggersButton);
        triggerTextFieldsPane.addRow(++row,
            triggerNameLabel, triggerNameField);
        triggerTextFieldsPane.addRow(++row,
            triggerPatternLabel, triggerPatternField.node());
        triggerTextFieldsPane.addRow(++row, oneTimeField);
        triggerTextFieldsPane.addRow(++row, commentsLabel);
        triggerTextFieldsPane.addRow(++row, commentsField);
        GridPane.setFillWidth(backToTriggersButton, false);
        GridPane.setMargin(backToTriggersButton, new Insets(0, 0, 6, 0));
        GridPane.setColumnSpan(backToTriggersButton, GridPane.REMAINING);
        GridPane.setHgrow(triggerNameField, Priority.ALWAYS);
        GridPane.setHgrow(triggerPatternField.node(), Priority.ALWAYS);
        GridPane.setColumnSpan(oneTimeField, GridPane.REMAINING);
        GridPane.setColumnSpan(commentsLabel, GridPane.REMAINING);
        GridPane.setColumnSpan(commentsField, GridPane.REMAINING);
        GridPane.setHgrow(commentsField, Priority.ALWAYS);
        GridPane.setVgrow(commentsField, Priority.ALWAYS);

        Node triggerButtonPane = createButtonPane(
            createTriggerButton,
            editTriggerButton,
            moveTriggerToTopButton,
            moveTriggerUpButton,
            moveTriggerDownButton,
            moveTriggerToBottomButton,
            removeTriggerButton);

        Node actionButtonPane = createButtonPane(
            createActionButton,
            moveActionUpButton,
            moveActionDownButton,
            removeActionButton);

        GridPane actionListPane = new GridPane();
        actionListPane.setPadding(new Insets(6));
        actionListPane.setHgap(12);
        actionListPane.setVgap(3);
        row = -1;
        actionListPane.addRow(++row, actionsLabel);
        actionListPane.addRow(++row, actionList, actionButtonPane);
        GridPane.setHgrow(actionList, Priority.ALWAYS);
        GridPane.setVgrow(actionList, Priority.ALWAYS);

        BorderPane actionFieldsPane = new BorderPane(actionDetailsPane,
            null, null, null, actionTypesPane);
        actionFieldsPane.setPadding(new Insets(6));
        BorderPane.setMargin(actionDetailsPane, new Insets(0, 0, 0, 24));

        actionFieldsPane.disableProperty().bind(selectedAction.isNull());

        SplitPane triggerFieldsPane = new SplitPane(
            triggerTextFieldsPane, actionListPane, actionFieldsPane);
        triggerFieldsPane.setOrientation(Orientation.VERTICAL);
        SplitPane.setResizableWithParent(triggerTextFieldsPane, false);
        SplitPane.setResizableWithParent(actionFieldsPane, false);

        GridPane triggerListHeadingPane = new GridPane();
        triggerListHeadingPane.setHgap(6);
        triggerListHeadingPane.addRow(0,
            triggersLabel, triggerTypeLabel, triggerTypeList);
        GridPane.setHgrow(triggersLabel, Priority.ALWAYS);

        BorderPane triggerListPane = new BorderPane(triggerList,
            triggerListHeadingPane, triggerButtonPane, null, null);
        BorderPane.setMargin(triggerButtonPane, new Insets(0, 0, 0, 6));
        BorderPane.setMargin(triggerListHeadingPane, new Insets(6, 0, 6, 0));
        triggerListPane.setPadding(new Insets(0, 6, 6, 6));

        Node connectionButtonPane = createButtonPane(
            connectButton,
            createConnectionButton,
            editConnectionButton,
            removeConnectionButton);

        HBox userPromptPane = new HBox(6,
            userPromptLabel, userPromptPatternField.node());
        userPromptPane.setAlignment(Pos.CENTER_LEFT);

        HBox passwordPromptPane = new HBox(6,
            passwordPromptButton, passwordPromptPatternField.node());
        HBox passwordAfterUserPane = new HBox(6,
            passwordAfterUserButton, passwordSeparatorField);
        passwordPromptPane.setAlignment(Pos.CENTER_LEFT);
        passwordAfterUserPane.setAlignment(Pos.CENTER_LEFT);

        GridPane loginPane = new GridPane();
        loginPane.setHgap(6);
        loginPane.setVgap(6);
        loginPane.addRow(0, userPromptPane);
        loginPane.addRow(1, passwordPromptPane);
        loginPane.addRow(2, passwordAfterUserPane);
        loginPane.setPadding(new Insets(6));

        GridPane colorsPane = new GridPane();
        colorsPane.setHgap(6);
        colorsPane.setVgap(6);
        colorsPane.addRow(0, useSystemColorsField);
        colorsPane.addRow(1,
            foregroundLabel, foregroundField,
            backgroundLabel, backgroundField,
            colorSample);
        GridPane.setColumnSpan(useSystemColorsField, GridPane.REMAINING);
        GridPane.setMargin(backgroundLabel, new Insets(0, 0, 0, 24));
        GridPane.setMargin(colorSample, new Insets(0, 0, 0, 24));
        colorsPane.setPadding(new Insets(6));

        VBox sslPane = new VBox(3, sslField, validCertsField);
        sslPane.setMaxWidth(Region.USE_PREF_SIZE);
        sslPane.setMaxHeight(Region.USE_PREF_SIZE);
        sslPane.setPadding(new Insets(6, 0, 6, 12));

        TabPane advancedTabPane = new TabPane(
            new Tab(res.getString("connection.tab.login"), loginPane),
            new Tab(res.getString("connection.tab.appearance"), colorsPane),
            new Tab(res.getString("connection.tab.triggers"), triggerListPane));
        advancedTabPane.getTabs().forEach(tab -> tab.setClosable(false));
        BorderPane advancedPane = new BorderPane(advancedTabPane);
        advancedPane.getStyleClass().add("advanced");
        advancedPane.setManaged(false);

        // Create inset border.
        BorderStroke stroke = new BorderStroke(null, null, null, null);
        Paint dark = stroke.getTopStroke();
        Paint light = dark;
        if (light instanceof Color)
        {
            light = ((Color) light).interpolate(Color.WHITE, 0.75);
        }
        stroke = new BorderStroke(
            dark, light, light, dark,
            BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
            BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
            null, null, new Insets(0));
        advancedTabPane.setBorder(new Border(stroke));

        Button showAdvancedButton = createButton(
            res.getString("connection.advanced"),
            () -> reveal(advancedPane));
        showAdvancedButton.disableProperty().bind(
            advancedPane.managedProperty());
        HBox advancedButtonPane = new HBox(showAdvancedButton);

        GridPane connectionFieldsPane = new GridPane();
        connectionFieldsPane.setHgap(6);
        connectionFieldsPane.setVgap(3);
        row = -1;
        connectionFieldsPane.addRow(++row, backToConnectionsButton);
        connectionFieldsPane.addRow(++row, nameLabel, nameField);
        connectionFieldsPane.addRow(++row, hostLabel, hostField);
        connectionFieldsPane.addRow(++row, portLabel, portField);
        connectionFieldsPane.addRow(++row, userLabel, userField);
        connectionFieldsPane.addRow(++row, passwordLabel, passwordField);
        connectionFieldsPane.add(sslPane, 2, row - 4, 1, 5);
        connectionFieldsPane.addRow(++row, advancedButtonPane);
        connectionFieldsPane.addRow(++row, advancedPane);
        GridPane.setColumnSpan(backToConnectionsButton, GridPane.REMAINING);
        GridPane.setFillWidth(backToConnectionsButton, false);
        GridPane.setMargin(backToConnectionsButton, new Insets(0, 0, 9, 0));
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(portField, Priority.ALWAYS);
        GridPane.setHgrow(userField, Priority.ALWAYS);
        GridPane.setHalignment(sslPane, HPos.CENTER);
        GridPane.setValignment(sslPane, VPos.CENTER);
        GridPane.setColumnSpan(advancedButtonPane, GridPane.REMAINING);
        GridPane.setMargin(advancedButtonPane, new Insets(6, 0, 0, 0));
        GridPane.setHgrow(advancedPane, Priority.ALWAYS);
        GridPane.setColumnSpan(advancedPane, GridPane.REMAINING);
        GridPane.setMargin(advancedPane, new Insets(6, 0, 0, 0));

        Region connectionPane = connectionFieldsPane;
        connectionPane.setPadding(new Insets(6));

        Region detailsPane = connectionPane;

        GridPane connectionListPane = new GridPane();
        connectionListPane.addRow(0, connectionListLabel);
        connectionListPane.addRow(1, connectionList, connectionButtonPane);
        GridPane.setHgrow(connectionList, Priority.ALWAYS);
        GridPane.setVgrow(connectionList, Priority.ALWAYS);
        GridPane.setMargin(connectionListLabel, new Insets(6, 3, 3, 3));
        GridPane.setMargin(connectionButtonPane, new Insets(0, 6, 6, 6));

        pages = Arrays.asList(
            connectionListPane, detailsPane, triggerFieldsPane);

        paginator = new Pagination(pages.size());
        paginator.setPageFactory(pages::get);
        // We do not want user navigating between pages directly.
        paginator.setOnKeyPressed(this::suppressNavigation);
        paginator.setOnSwipeLeft(Event::consume);
        paginator.setOnSwipeRight(Event::consume);
        paginator.setOnSwipeUp(Event::consume);
        paginator.setOnSwipeDown(Event::consume);

        // Temporarily place all children invisibly behind paginator,
        // so they'll get laid out and their sizes will properly contribute
        // to the paginator's size.
        StackPane pagesPane = new StackPane(pages.toArray(new Node[0]));
        pagesPane.setVisible(false);
        StackPane paginatorPane = new StackPane(paginator, pagesPane);
        Node contents = paginatorPane;

        initialFocus = Arrays.asList(
            connectionList, nameField, triggerNameField);

        dialog = new Dialog<>();
        dialog.initOwner(parent);

        dialog.setTitle(res.getString("connection.title"));
        dialog.getDialogPane().setContent(contents);
        dialog.getDialogPane().getButtonTypes().setAll(
            ButtonType.CLOSE);
        dialog.setResizable(true);

        dialog.getDialogPane().disableProperty().bind(
            persistor.runningProperty());

        dialog.getDialogPane().getStyleClass().add("connections");
        dialog.getDialogPane().getStylesheets().add(
            ConnectionManager.class.getResource("style.css").toString());

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (okButton instanceof Labeled)
        {
            ((Labeled) okButton).setText(res.getString("connection.ok"));
        }

/*
        dialog.getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(
            ActionEvent.ACTION, this::validateFields);
*/

        //okButton.addEventFilter(ActionEvent.ACTION, this::validateFields);
/*
        IntegerExpression portValue =
            IntegerExpression.integerExpression(portFormatter.valueProperty());
        okButton.disableProperty().bind(
            hostField.textProperty().isEmpty().or(
            portField.textProperty().isEmpty().or(
            portValue.lessThanOrEqualTo(0).or(portValue.greaterThan(65535)))));
*/

/*
        dialog.setOnShown(e -> //connectionList.requestFocus());
javafx.application.Platform.runLater(() -> connectionList.requestFocus()));
*/
    }

    private Button createButton(String text,
                                Runnable action)
    {
        Button button = new Button(text);
        button.setMnemonicParsing(true);
        button.setOnAction(e -> action.run());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    private RadioButton createRadioButton(String text)
    {
        RadioButton button = new RadioButton(text);
        button.setMnemonicParsing(true);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    private Node createButtonPane(Node... buttons)
    {
        return new HBox(new VBox(6, buttons));
    }

    private HBox createBaselineAlignedRow(Node... nodes)
    {
        HBox row = new HBox(6, nodes);
        row.setAlignment(Pos.BASELINE_LEFT);
        return row;
    }

    private void reveal(Node node)
    {
        node.setManaged(true);
        node.getScene().getWindow().sizeToScene();
    }

    private Node createTablePlaceholder(String text)
    {
        TextFlow placeholder = new TextFlow(new Text(text));
        placeholder.setTextAlignment(TextAlignment.CENTER);
        placeholder.setMaxHeight(Region.USE_PREF_SIZE);
        return placeholder;
    }

    private void selectFirst(TableView<?> table)
    {
        if (table.getItems() != null && !table.getItems().isEmpty())
        {
            table.getSelectionModel().selectFirst();
            table.getFocusModel().focus(0);
        }
    }

    private static boolean isContiguous(Collection<Integer> selection)
    {
        logger.entering(ConnectionManager.class.getName(), "isContiguous",
            selection);

        SortedSet<Integer> indices = new TreeSet<>(selection);

        boolean contiguous = !indices.isEmpty() &&
            (indices.last() - indices.first() + 1 == indices.size());

        logger.exiting(ConnectionManager.class.getName(), "isContiguous",
            contiguous);
        return contiguous;
    }

    private BooleanExpression cannotMoveUp(TableView<?> table)
    {
        ObservableList<Integer> selection =
            table.getSelectionModel().getSelectedIndices();
        return Bindings.createBooleanBinding(() ->
            selection.contains(0) || !isContiguous(selection),
            selection, table.itemsProperty());
    }

    private BooleanExpression cannotMoveDown(TableView<?> table)
    {
        ObservableList<Integer> selection =
            table.getSelectionModel().getSelectedIndices();
        return Bindings.createBooleanBinding(() ->
                table.getItems() == null ||
                selection.contains(table.getItems().size() - 1) ||
                !isContiguous(selection),
            selection, table.itemsProperty());
    }

    private String defaultNewConnectionName()
    {
        return defaultName(defaultConnectionNameFormat,
            connectionList.getItems() == null ? Stream.empty() :
            connectionList.getItems().stream().map(ConnectionInfo::getName));
    }

    private String defaultNewTriggerName()
    {
        return defaultName(defaultTriggerNameFormat,
            triggerList.getItems() == null ? Stream.empty() :
            triggerList.getItems().stream().map(Trigger::getName));
    }

    private static String defaultName(MessageFormat nameFormat,
                                      Stream<String> existingNames)
    {
        Set<Integer> existingNumbers = new TreeSet<>();

        ParsePosition pos = new ParsePosition(0);
        existingNames.forEach(name ->
        {
            pos.setIndex(0);
            pos.setErrorIndex(-1);
            Object[] values = nameFormat.parse(name, pos);

            if (values != null)
            {
                existingNumbers.add(((Number) values[0]).intValue());
            }
        });

        int number = 0;
        for (int existingNumber : existingNumbers)
        {
            if (existingNumber > number + 1)
            {
                break;
            }
            number = existingNumber;
        }

        return nameFormat.format(new Object[] { number + 1 });
    }

    private void updateTriggerList(ConnectionInfo connection)
    {
        if (connection != null) {
            triggerList.itemsProperty().bind(
                Bindings.valueAt(connection.getAllTriggers(), 
                    triggerTypeList.valueProperty()));
        }
    }

    private void updateAction(Action action,
                              Toggle button)
    {
        if (action != null && button != null)
        {
            logger.fine(() ->
                "Setting type=" + button.getUserData() + " on " + action);
            action.setType((Action.Type) button.getUserData());
        }
    }

    private void suppressNavigation(KeyEvent event)
    {
        KeyCode code = event.getCode();
        if (code != null && code.isNavigationKey())
        {
            event.consume();
        }
    }

/*
    private void validateFields(ActionEvent event)
    {
        // TODO
        String textValue = patternEditField.getText();
        PatternType typeValue = typeField.getValue();

        try
        {
            getPatternAsRegex(textValue, typeValue);
        }
        catch (PatternSyntaxException e)
        {
            event.consume();
            errorText.setText(e.getMessage());
            errorText.setVisible(true);
        }
    }
*/

/*
    private TextFormatter.Change restrictPortChars(TextFormatter.Change change)
    {
        if (change.isDeleted())
        {
            return change;
        }

        change = change.clone();
        String text = change.getText();

        // Digits only.
        text = text.replaceAll("\\D", "");
        change.setText(text);

        // Limit length.
        int newLen = change.getControlNewText().length();
        if (newLen > 5)
        {
            text = text.substring(0, newLen - 5);
            change.setText(text);
        }

        return change;
    }
*/

    ConnectionInfo show()
    {
        persistor.setOperation(ConnectionInfoPersistor.Operation.LOAD);
        persistor.restart();

        connectToSelection = false;
        showPage(0);
        dialog.showAndWait();

        ConnectionInfo info =
            connectionList.getSelectionModel().getSelectedItem();

        if (!persistor.isRunning())
        {
            logger.config("Saving connections...");
            persistor.setOperation(ConnectionInfoPersistor.Operation.SAVE);
            // This is not the same as restart(), which would cancel any
            // currently running Task.  We want to be sure no task is
            // currently running, rather than blindly canceling it.
            persistor.reset();
            persistor.start();
        }

        logger.fine(() -> "connectToSelection=" + connectToSelection);
        return (connectToSelection ? info : null);
    }

    private void showLoadFailure()
    {
        loadFailedHandler.showError(persistor.getException(),
            connectionList.getScene().getWindow());
    }

    private void updateMnemonicParsing(Node node)
    {
        if (node.isVisible())
        {
            LabelFactory.restoreMnemonicParsing(node);
        }
        else
        {
            LabelFactory.saveAndDisableMnemonicParsing(node);
        }
    }

    private void showPage(int pageNumber)
    {
        paginator.setCurrentPageIndex(pageNumber);
        for (int i = pages.size() - 1; i >= 0; i--)
        {
            Node page = pages.get(i);
            if (i == pageNumber)
            {
                LabelFactory.restoreMnemonicParsing(page);
            }
            else
            {
                LabelFactory.saveAndDisableMnemonicParsing(page);
            }
        }

        Platform.runLater(() -> initialFocus.get(pageNumber).requestFocus());
    }

    private void hideConnectionFields()
    {
        showPage(0);
    }

    private void hideTriggerFields()
    {
        String enteredName = triggerNameField.getText();
        if (triggerList.getItems().stream()
            .filter(t -> Objects.equals(t.getName(), enteredName)).count() > 1)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(duplicateTriggerTitle);
            alert.setHeaderText(duplicateTriggerHeader);
            alert.setContentText(duplicateTriggerMessageFormat.format(
                new Object[] { enteredName }));
            alert.show();
            return;
        }

        // Validate sound paths.
        List<Action> invalidSoundActions = new ArrayList<>();
        for (Action action : actionList.getItems())
        {
            if (action.getType() == SOUND)
            {
                String path = Objects.toString(action.getDetail(), "");
                try
                {
                    new Media(Paths.get(path).toUri().toString());
                }
                catch (MediaException e)
                {
                    logger.log(Level.INFO,
                        "Invalid sound file \"" + action.getDetail() + "\"",
                        e);
                    invalidSoundActions.add(action);
                }
            }
        }
        if (!invalidSoundActions.isEmpty())
        {
            Object[] formatArgs = { invalidSoundActions.size() };

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                invalidSoundMessageFormat.format(formatArgs),
                ButtonType.YES, ButtonType.NO);
            alert.setTitle(invalidSoundTitle);
            alert.setHeaderText(invalidSoundHeaderFormat.format(formatArgs));

            Optional<ButtonType> response = alert.showAndWait();
            if (!response.filter(r -> r == ButtonType.YES).isPresent())
            {
                return;
            }
        }

        showPage(1);
    }

    private void connect()
    {
        connectToSelection = true;
        dialog.hide();
    }

    private void createConnection()
    {
        ConnectionInfo info = new ConnectionInfo();
        info.setName(defaultNewConnectionName());
        connectionList.getItems().add(info);
        connectionList.getSelectionModel().select(info);
        updateColors();
        showPage(1);
    }

    private boolean isEditEvent(MouseEvent event)
    {
        return (event.getButton() == MouseButton.PRIMARY &&
            event.getClickCount() == 2 && !event.isPopupTrigger() &&
            !event.isShiftDown() && !event.isControlDown() &&
            !event.isAltDown() && !event.isMetaDown());
    }

    private boolean isEditEvent(KeyEvent event)
    {
        return (event.getCode() == KeyCode.ENTER);
    }

    private void edit(MouseEvent event,
                      Runnable editImpl)
    {
        edit(event, this::isEditEvent, editImpl);
    }

    private void edit(KeyEvent event,
                      Runnable editImpl)
    {
        edit(event, this::isEditEvent, editImpl);
    }

    private <E extends Event> void edit(E event,
                                        Predicate<E> isEdit,
                                        Runnable editImpl)
    {
        if (isEdit.test(event))
        {
            TableView<?> table = (TableView<?>) event.getSource();
            if (table.getSelectionModel().getSelectedIndices().size() == 1)
            {
                editImpl.run();
            }
        }
    }

    private void editConnection()
    {
        updateColors();
        showPage(1);
    }

    /**
     * @param <T> type of item in TableView
     *
     * @param list table from which to delete items
     * @param getName determines human-readable name of each item
     * @param confirmTitle title of confirmation dialog
     * @param confirmMessageFormat message in confirmation dialog.
     *                             Expects two arguments:
     *                             number of items selected, and name of
     *                             first item selected.
     */
    private <T> void delete(TableView<T> list,
                            Function<T, ?> getName,
                            String confirmTitle,
                            String confirmHeader,
                            MessageFormat confirmMessageFormat)
    {
        delete(list, getName,
            confirmTitle, confirmHeader, confirmMessageFormat,
            selection -> list.getItems().removeAll(selection));
    }

    /**
     * @param <T> type of item in TableView
     *
     * @param list table from which to delete items
     * @param getName determines human-readable name of each item
     * @param confirmTitle title of confirmation dialog
     * @param confirmMessageFormat message in confirmation dialog.
     *                             Expects two arguments:
     *                             number of items selected, and name of
     *                             first item selected.
     * @param deleteImpl actual deletion; gets passed selected items
     */
    private <T> void delete(TableView<T> list,
                            Function<T, ?> getName,
                            String confirmTitle,
                            String confirmHeader,
                            MessageFormat confirmMessageFormat,
                            Consumer<Collection<T>> deleteImpl)
    {
        Collection<T> selection = list.getSelectionModel().getSelectedItems();

        String text = confirmMessageFormat.format(new Object[] {
            selection.size(), getName.apply(selection.iterator().next())
        });

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, text,
            ButtonType.YES, ButtonType.NO);
        confirmation.setTitle(confirmTitle);
        confirmation.setHeaderText(confirmHeader);
        confirmation.initOwner(list.getScene().getWindow());

        Optional<ButtonType> response = confirmation.showAndWait();
        if (response.filter(r -> r.equals(ButtonType.YES)).isPresent())
        {
            logger.fine("Delete confirmed");
            deleteImpl.accept(new ArrayList<T>(selection));
        }
    }

    private void deleteConnection()
    {
        delete(connectionList, ConnectionInfo::getName,
            deleteConnectionConfirmationTitle,
            deleteConnectionConfirmationHeader,
            deleteConnectionConfirmationMessageFormat);
    }

    private void showSoundFileChooser()
    {
        File soundFile = soundFileChooser.showOpenDialog(
            actionList.getScene().getWindow());
        if (soundFile != null)
        {
            soundFileField.setText(soundFile.toString());
        }
    }

    private void createTrigger()
    {
        Trigger trigger = new Trigger(defaultNewTriggerName());
        triggerList.getItems().add(trigger);
        triggerList.getSelectionModel().select(trigger);
        showPage(2);
    }

    private void editTrigger()
    {
        showPage(2);
    }

    private void moveTriggersBy(int increment)
    {
        int index = triggerList.getSelectionModel().getSelectedIndex();

        ObservableList<Trigger> triggers = displayedTriggers();

        Collection<Trigger> triggersToMove = new ArrayList<>(
            triggerList.getSelectionModel().getSelectedItems());

        triggers.removeAll(triggersToMove);
        triggers.addAll(index + increment, triggersToMove);

        triggerList.getSelectionModel().clearSelection();
        triggerList.getSelectionModel().selectRange(
            index + increment,
            index + increment + triggersToMove.size());
    }

    private void moveTriggerUp()
    {
        moveTriggersBy(-1);
    }

    private void moveTriggerDown()
    {
        moveTriggersBy(1);
    }

    private void moveTriggerToTop()
    {
        int index = triggerList.getSelectionModel().getSelectedIndex();

        ObservableList<Trigger> triggers = displayedTriggers();

        Collection<Trigger> triggersToMove = new ArrayList<>(
            triggerList.getSelectionModel().getSelectedItems());

        triggers.removeAll(triggersToMove);
        triggers.addAll(0, triggersToMove);

        triggerList.getSelectionModel().clearSelection();
        triggerList.getSelectionModel().selectRange(0, triggersToMove.size());
    }

    private void moveTriggerToBottom()
    {
        int index = triggerList.getSelectionModel().getSelectedIndex();

        ObservableList<Trigger> triggers = displayedTriggers();

        Collection<Trigger> triggersToMove = new ArrayList<Trigger>(
            triggerList.getSelectionModel().getSelectedItems());

        triggers.removeAll(triggersToMove);
        triggers.addAll(triggersToMove);

        triggerList.getSelectionModel().clearSelection();
        triggerList.getSelectionModel().selectRange(
            triggers.size() - triggersToMove.size(),
            triggers.size());
    }

    private ObservableList<Trigger> displayedTriggers()
    {
        logger.entering(ConnectionManager.class.getName(), "displayedTriggers");

        ConnectionInfo selectedConnection =
            connectionList.getSelectionModel().getSelectedItem();
        return selectedConnection.getAllTriggers().get(
            triggerTypeList.getValue());
    }

    private void deleteTrigger()
    {
        logger.entering(ConnectionManager.class.getName(), "deleteTrigger");

        delete(triggerList, Trigger::getName,
            deleteTriggerConfirmationTitle,
            deleteTriggerConfirmationHeader,
            deleteTriggerConfirmationMessageFormat,
            selection -> displayedTriggers().removeAll(selection));
    }

    private void createAction()
    {
        Action action = new Action();
        actionList.getItems().add(action);
        actionList.getSelectionModel().select(action);
    }

    private void moveActionsBy(int increment)
    {
        int index = actionList.getSelectionModel().getSelectedIndex();

        ObservableList<Action> actions = actionList.getItems();

        Collection<Action> actionsToMove = new ArrayList<>(
            actionList.getSelectionModel().getSelectedItems());

        actions.removeAll(actionsToMove);
        actions.addAll(index + increment, actionsToMove);

        actionList.getSelectionModel().clearSelection();
        actionList.getSelectionModel().selectRange(
            index + increment,
            index + increment + actionsToMove.size());
    }

    private void moveActionUp()
    {
        moveActionsBy(-1);
    }

    private void moveActionDown()
    {
        moveActionsBy(1);
    }

    private void deleteAction()
    {
        delete(actionList, Action::getSummary,
            deleteActionConfirmationTitle,
            deleteActionConfirmationHeader,
            deleteActionConfirmationMessageFormat);
    }

    private static String escapeHTML(String text)
    {
        Formatter html =
            new Formatter(new StringBuilder(text.length()));
        text.codePoints().forEachOrdered(c -> html.format(
            c < 32 || c == '<' || c == '>' || c == '&' ? "&#%d;" : "%c", c));

        return html.toString();
    }

    private void updateColors()
    {
        WebView view = colorSampleField.view;
        if (view.getEngine().getDocument() == null)
        {
            return;
        }

        Element textSpan =
            view.getEngine().getDocument().getElementById("body");
        textSpan.setAttribute("style",
            useSystemColorsField.isSelected() ? "" :
            Chat.toStyle(
                foregroundField.getValue(), backgroundField.getValue()));
    }
}
