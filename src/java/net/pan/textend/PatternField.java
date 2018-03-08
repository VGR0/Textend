package net.pan.textend;

import java.text.MessageFormat;;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import java.util.ResourceBundle;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.event.ActionEvent;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Window;

class PatternField
{
    private final StringProperty pattern = new SimpleStringProperty();

    private final ObjectProperty<PatternType> type =
        new SimpleObjectProperty<>(PatternType.TEXT_CASELESS);

    //private final GridPane contents;
    private final HBox contents;

    private final Label patternLabel;

    private Dialog<ButtonType> dialog;

    private final GridPane dialogContents;

    private final TextField patternEditField;

    private final ComboBox<PatternType> typeField;

    private final Text errorText;

    private final String dialogTitle;

    private final MessageFormat unclosedGlobClassMessageFormat;

    PatternField(String changeButtonText)
    {
        patternLabel = new Label();
        patternLabel.getStyleClass().addAll("pattern", "uneditable");
        patternLabel.setMaxWidth(Double.MAX_VALUE);
        patternLabel.textProperty().bind(patternProperty());

        TextField patternLabelSizer = new TextField();
        patternLabelSizer.setPrefColumnCount(16);
        patternLabelSizer.setVisible(false);

        StackPane patternLabelPane =
            new StackPane(patternLabelSizer, patternLabel);

        Button changeButton = new Button(changeButtonText);
        changeButton.setMnemonicParsing(true);
        changeButton.setOnAction(e -> showEditDialog());
        changeButton.setMinWidth(Region.USE_PREF_SIZE);

        contents = new HBox(6, patternLabelPane, changeButton); // TODO
        HBox.setHgrow(patternLabelPane, Priority.ALWAYS);
/*
        contents = new GridPane();
        contents.setHgap(6);
        contents.addRow(0, patternLabelPane, changeButton);
        GridPane.setHgrow(patternLabelPane, Priority.ALWAYS);
*/

        patternEditField = new TextField();
        patternEditField.setPrefColumnCount(24);

        ResourceBundle res = ResourceBundle.getBundle(
            PatternField.class.getPackage().getName() + ".Localization");

        typeField = EnumListCell.createEnumList(PatternType.class, res);

        errorText = new Text();
        errorText.getStyleClass().add("pattern-error");
        // Give errorText some content so its initial size is reasonable.
        try
        {
            Pattern.compile("(");
        }
        catch (PatternSyntaxException e)
        {
            errorText.setText(e.getMessage());
        }

        dialogTitle = res.getString("pattern.title");

        Label patternLabel = LabelFactory.createLabel(
            res.getString("pattern.text"), patternEditField);

        Label typeLabel = LabelFactory.createLabel(
            res.getString("pattern.type"), typeField);

        dialogContents = new GridPane();
        dialogContents.getStylesheets().add(
            PatternField.class.getResource("style.css").toString());
        dialogContents.setHgap(6);
        dialogContents.setVgap(6);
        dialogContents.addRow(0, patternLabel, patternEditField);
        dialogContents.addRow(1, typeLabel, typeField);
        dialogContents.add(errorText, 1, 2);
        GridPane.setHgrow(patternEditField, Priority.ALWAYS);

        unclosedGlobClassMessageFormat = new MessageFormat(
            res.getString("pattern.unclosedGlobClass"));
    }

    Node node()
    {
        return contents;
    }

    public StringProperty patternProperty()
    {
        return pattern;
    }

    public String getPattern()
    {
        return pattern.get();
    }

    public void setPattern(String pattern)
    {
        this.pattern.set(pattern);
    }

    public ObjectProperty<PatternType> typeProperty()
    {
        return type;
    }

    public PatternType getType()
    {
        return type.get();
    }

    public void setType(PatternType type)
    {
        //Objects.requireNonNull(type, "Type cannot be null");
        this.type.set(type);
    }

    private void validatePattern(ActionEvent event)
    {
        String textValue = patternEditField.getText();
        PatternType typeValue = typeField.getValue();

        try
        {
            Trigger.patternToRegex(textValue, typeValue,
                unclosedGlobClassMessageFormat);
        }
        catch (PatternSyntaxException e)
        {
            event.consume();
            errorText.setText(e.getMessage());
            errorText.setVisible(true);
        }
    }

    private void showEditDialog()
    {
        if (dialog == null)
        {
            dialog = new Dialog<>();
            dialog.initOwner(contents.getScene().getWindow());
            dialog.setTitle(dialogTitle);
            dialog.getDialogPane().setContent(dialogContents);
            dialog.getDialogPane().getButtonTypes().setAll(
                ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
                ActionEvent.ACTION, this::validatePattern);
            dialog.setResizable(true);
            dialog.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
            dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            // Workaround to ensure minimum size of window.
            //Bindings.select(dialog.getDialogPane(), "scene", "window").addListener(
            //    (obs, old, window) -> addMinSizeListeners((Window) window));
        }

        patternEditField.setText(getPattern());
        typeField.setValue(getType());
        errorText.setVisible(false);
        dialog.getDialogPane().autosize();

        if (dialog.showAndWait().filter(r -> r == ButtonType.OK).isPresent())
        {
            setPattern(patternEditField.getText());
            setType(typeField.getValue());
        }
    }

/*
    private void addMinSizeListeners(Window window)
    {
        if (window == null)
        {
            return;
        }

        window.xProperty().addListener((obs, old, x) -> requireMinSize(window));
        window.yProperty().addListener((obs, old, y) -> requireMinSize(window));
    }

    private void requireMinSize(Window window)
    {
System.out.println("Window changed size");
        Node root = window.getScene().getRoot();

        double width = root.getLayoutBounds().getWidth();
        double minWidth = root.minWidth(-1);
        if (width < minWidth)
        {
            window.setWidth(minWidth + (window.getWidth() - width));
        }

        double height = root.getLayoutBounds().getHeight();
        double minHeight = root.minHeight(-1);
        if (height < minHeight)
        {
            window.setHeight(minHeight + (window.getHeight() - height));
        }
    }
*/
}
