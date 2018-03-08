package net.pan.textend;

// TODO: input history
// TODO: webView.onScroll

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.MessageFormat;

import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import java.nio.file.Paths;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import java.util.Formatter;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.awt.EventQueue;
import java.awt.Toolkit;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Side;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tab;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaException;

import javafx.stage.Window;

import netscape.javascript.JSObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.ls.LSSerializer;

/**
 * User interface and trigger engine for a single connection.
 */
public class Chat
{
    private static final Logger logger =
        Logger.getLogger(Chat.class.getName());

    static final int DEFAULT_OUTPUT_LINE_COUNT = 30;

    private static final int HISTORY_SIZE = 1000;

    /**
     * Time to wait before sending user/password in response to prompt.
     * I don't know why, but sending the response too soon sometimes causes it
     * to be ignored.
     */
    private static final long RESPONSE_DELAY = Math.max(0,
        Integer.getInteger(Chat.class.getName() + ".responseDelay", 250));

    private static class TriggerResult
    {
        boolean hidden;
        Element timestamp;
    }

    private final Tab tab;

    private final WebView outputView;

    private final WebEngine outputField;

    private final ContextMenu outputMenu;

    private final MenuItem copyItem;

    private final MenuItem copyLinkItem;

    private final TextArea inputField;

    private final TextAttributes textAttributes = new TextAttributes();

    private volatile TelnetConnection connection;

    private ConnectionInfo connectionInfo;

    private final CharsetEncoder charsetEncoder;

    private final ExecutorService readExecutor = 
        Executors.newSingleThreadExecutor();

    private final ScheduledExecutorService sendQueue =
        Executors.newSingleThreadScheduledExecutor();

    private final ScheduledExecutorService beepTimer =
        Executors.newSingleThreadScheduledExecutor();

    // As far as I know, it's not possible to support this (yet).
    private boolean scrolledToEnd = true;

    private final MessageFormat closedTitleFormat;
    private final String closedTitleSuffix;

    private final URIDisplayer uriDisplayer;

    private final ErrorHandler errorHandler;

    private Matcher csiMatcher =
        Pattern.compile("\033\\[[^a-zA-Z]*[a-zA-Z]").matcher("");

    /**
     * Element ID in HTML document of {@code <span>} at the end of the
     * document, which exists solely to force the WebView to render a
     * newline after the final {@code <br>}.
     */
    private Element finalLine;

    // Eventually, these may be configurable.

    private String unicodeStart = "{";

    private String unicodeEnd = "}";

    private String timestampFormat;

    // Other data

    private final String confirmCloseTitle;
    private final String confirmCloseHeader;
    private final MessageFormat confirmCloseMessageFormat;

    // State

    private boolean userTriggerTripped;

    private boolean passwordTriggerTripped;

    private final Collection<Trigger> oneTimeTriggersTripped = new HashSet<>();

    private final ReadOnlyBooleanWrapper copyable =
        new ReadOnlyBooleanWrapper();

    private final ReadOnlyBooleanWrapper focused =
        new ReadOnlyBooleanWrapper();

    private final ReadOnlyBooleanWrapper connected =
        new ReadOnlyBooleanWrapper();

    /**
     * Somewhat of a misnomer, since a context menu can also be brought up by
     * a keyboard action.
     */
    private String rightClickedURI;

    private final History<String> history = new History<>(HISTORY_SIZE);

    Chat(ConnectionInfo connectionInfo,
         URIDisplayer uriDisplayer)
    {
        this.connectionInfo = Objects.requireNonNull(connectionInfo,
            "ConnectionInfo cannot be null");
        this.uriDisplayer = Objects.requireNonNull(uriDisplayer,
            "URIDisplayer cannot be null");

        Charset charset;
        String charsetName = connectionInfo.getCharset();
        if (charsetName == null)
        {
            charset = StandardCharsets.US_ASCII;
        }
        else
        {
            try
            {
                charset = Charset.forName(charsetName);
            }
            catch (IllegalArgumentException e)
            {
                logger.log(Level.WARNING, "Invalid charset" +
                    " in ConnectionInfo: \"" + charsetName + "\"" +
                    "; falling back on ASCII.", e);
                charset = StandardCharsets.US_ASCII;
            }
        }
        charsetEncoder = charset.newEncoder();

        ResourceBundle res = ResourceBundle.getBundle(
            Chat.class.getPackage().getName() + ".Localization");

        timestampFormat = res.getString("timestamp.format");

        closedTitleFormat = new MessageFormat(res.getString("tab.closed.name"));
        closedTitleSuffix = res.getString("tab.closed.status");

        confirmCloseTitle = res.getString("tab.confirmClose.title");
        confirmCloseHeader = res.getString("tab.confirmClose.header");
        confirmCloseMessageFormat = new MessageFormat(
            res.getString("tab.confirmClose.message"));

        errorHandler = ErrorHandler.createNetworkErrorInstance(connectionInfo);

        copyItem = new MenuItem(res.getString("copy"));
        copyLinkItem = new MenuItem(res.getString("copyLink"));

        outputMenu = new ContextMenu(copyItem, copyLinkItem);
        outputMenu.setAnchorLocation(
            ContextMenu.AnchorLocation.CONTENT_TOP_LEFT);

        String colorStyle = "";
        if (!connectionInfo.isUseSystemColors())
        {
            Color foreground = connectionInfo.getForeground();
            Color background = connectionInfo.getBackground();
            if (foreground != null && background != null)
            {
                colorStyle = " style='" + toStyle(foreground, background) + "'";
            }
        }

        outputView = new WebView();
        outputView.setContextMenuEnabled(false);
        outputView.setOnKeyTyped(this::redirectToInputField);
        outputView.setOnContextMenuRequested(this::showOutputContextMenu);

        outputField = outputView.getEngine();
        outputField.documentProperty().addListener((obs, old, doc) ->
            ((EventTarget) doc).addEventListener(
                "selectionchange",
                e -> Platform.runLater(() -> updateCopyable()),
                false));
        outputField.loadContent(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\""
                               + " \"http://www.w3.org/TR/html4/strict.dtd\">"
            + "<html><body" + colorStyle + ">"
            + "<pre id='body' style='white-space: pre-wrap;'></pre>"
            + "</body></html>");

        copyItem.setOnAction(e -> copy());
        copyLinkItem.setOnAction(e -> setClipboardText(rightClickedURI));

        inputField = new TextArea();
        inputField.setPrefRowCount(3);
        inputField.setPrefColumnCount(60);
        inputField.setWrapText(true);
        inputField.setOnKeyPressed(this::processEnter);
        inputField.disableProperty().bind(connected.not());

        TextArea outputSizer = new TextArea();
        outputSizer.setPrefRowCount(DEFAULT_OUTPUT_LINE_COUNT);
        // This doesn't matter;  inputField determines the overall width.
        outputSizer.setPrefColumnCount(20);
        outputSizer.setVisible(false);

        StackPane outputArea = new StackPane(outputView, outputSizer);

        SplitPane.setResizableWithParent(outputArea, true);
        SplitPane.setResizableWithParent(inputField, false);
        SplitPane splitPane = new SplitPane(outputArea, inputField);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.9);

        tab = new Tab(
            MessageFormat.format(res.getString("tab.format"),
                connectionInfo.getName()),
            splitPane);
        tab.setUserData(this);
        tab.selectedProperty().addListener(
            (obs, old, selected) -> giveFocusToInput());

        Platform.runLater(() -> inputField.requestFocus());

        tab.setOnCloseRequest(e -> confirmClose(e));
        tab.setOnClosed(e -> close());

        inputField.focusedProperty().addListener(
            (obs, old, focused) -> updateCopyable());
        inputField.selectedTextProperty().addListener(
            (obs, old, text) -> updateCopyable());
        outputView.focusedProperty().addListener(
            (obs, old, focused) -> updateCopyable());

        ObjectExpression<Node> focusOwner =
            Bindings.select(tab.getContent().sceneProperty(), "focusOwner");
        focused.bind(Bindings.createBooleanBinding(
            () -> contains(focusOwner.get()),
            focusOwner));
    }

    private static void toStyle(Color color,
                                String cssPropertyName,
                                Formatter style)
    {
        if (color != null)
        {
            double red = color.getRed() * 100;
            double green = color.getGreen() * 100;
            double blue = color.getBlue() * 100;
            style.format("%s: rgb(%s%%, %s%%, %s%%);" +
                "%s: rgba(%s%%, %s%%, %s%%, %s%%);",
                cssPropertyName, red, green, blue,
                cssPropertyName, red, green, blue,
                color.getOpacity() * 100);
        }
    }

    static String toStyle(Color foreground,
                          Color background)
    {
        Formatter style = new Formatter();
        toStyle(foreground, "color", style);
        toStyle(background, "background-color", style);
        return style.toString();
    }

    private void giveFocusToInput()
    {
        if (tab.isSelected())
        {
            logger.fine("Tab selected; moving keyboard focus to input field.");

            ScheduledExecutorService focusTimer =
                Executors.newSingleThreadScheduledExecutor();
            Runnable focuser = new Runnable()
            {
                @Override
                public void run()
                {
                    if (inputField.isFocused())
                    {
                        focusTimer.shutdown();
                    }
                    inputField.requestFocus();
                }
            };
            focusTimer.scheduleWithFixedDelay(
                () -> Platform.runLater(focuser),
                0, 500, TimeUnit.MILLISECONDS);
        };
    }

    private void redirectToInputField(KeyEvent event)
    {
        inputField.requestFocus();
        Event.fireEvent(inputField, event.copyFor(inputField, inputField));
    }

    public ReadOnlyBooleanProperty focusedProperty()
    {
        return focused;
    }

    public boolean isFocused()
    {
        return focused.get();
    }

    public ReadOnlyBooleanProperty connectedProperty()
    {
        return connected;
    }

    public boolean isConnected()
    {
        return connected.get();
    }

    private static void setClipboardText(Object text)
    {
        Clipboard.getSystemClipboard().setContent(
            Collections.singletonMap(DataFormat.PLAIN_TEXT, text.toString()));
    }

    void copy()
    {
        setClipboardText(outputField.executeScript("window.getSelection();"));
    }

    private void updateCopyable()
    {
        if (inputField.isFocused())
        {
            copyable.set(inputField.selectedTextProperty().isNotEmpty().get());
        }
        else if (outputView.isFocused())
        {
            copyable.set(canCopyOutput());
        }
        else
        {
            copyable.set(false);
        }
    }

    public ReadOnlyBooleanProperty copyableProperty()
    {
        return copyable;
    }

    public boolean isCopyable()
    {
        return copyable.get();
    }

    private boolean canCopyOutput()
    {
        Object selection =
            outputField.executeScript("window.getSelection();");
        return (selection != null && !selection.toString().isEmpty());
    }

    private void showOutputContextMenu(ContextMenuEvent event)
    {
        event.consume();

        copyItem.setDisable(!canCopyOutput());

        copyLinkItem.setVisible(false);

        Window window = outputView.getScene().getWindow();

        if (event.isKeyboardTrigger())
        {
            // Position menu at focused element, or if there isn't one,
            // at the top left corner.

            Object focusedElementObj =
                outputField.executeScript("document.activeElement;");
            if (focusedElementObj instanceof Element)
            {
                Element focusedElement = (Element) focusedElementObj;
                if (focusedElement.getTagName().equalsIgnoreCase("a"))
                {
                    rightClickedURI = focusedElement.getTextContent();
                    copyLinkItem.setVisible(true);
                }

                JSObject rect = (JSObject) outputField.executeScript(
                    "document.activeElement.getBoundingClientRect();");
                Number top = (Number) rect.getMember("top");
                Number left = (Number) rect.getMember("left");
                Point2D screen = outputView.localToScreen(
                    left.doubleValue(), top.doubleValue());
                outputMenu.show(window, screen.getX(), screen.getY());
            }
            else
            {
                //outputMenu.show(outputView, Side.TOP, 0, 0);
                Point2D screen = outputView.localToScreen(0, 0);
                outputMenu.show(window, screen.getX(), screen.getY());
            }
        }
        else
        {
            Object elementObj = outputField.executeScript(
                String.format("document.elementFromPoint(%d,%d);",
                    (int) event.getX(), (int) event.getY()));
            if (elementObj instanceof Element)
            {
                Element element = (Element) elementObj;
                if (element.getTagName().equalsIgnoreCase("a"))
                {
                    rightClickedURI = element.getTextContent();
                    copyLinkItem.setVisible(true);
                }
            }

            outputMenu.show(window, event.getScreenX(), event.getScreenY());
        }
    }

    void connect()
    {
        String host = connectionInfo.getHost();
        int port = connectionInfo.getPort();
        boolean ssl = connectionInfo.isSSL();
        SSLProtocol protocol = connectionInfo.getSSLProtocol();
        boolean requireValidCert = connectionInfo.getRequireValidCertificate();

        String threadName = connectionInfo.toString();

        Runnable connectionInitializer = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    connection = TelnetConnection.create(host, port,
                        charsetEncoder.charset(),
                        ssl, protocol, requireValidCert,
                        line -> appendOutputLater(line));
                    Platform.runLater(() -> connected.set(true));

                    readExecutor.submit(connection).get();

                    // If we get here, connection closed gracefully.
                    Platform.runLater(() -> showAsClosed());
                }
                catch (IOException | IllegalArgumentException e)
                {
                    logger.log(Level.WARNING,
                        "Couldn't connect to " + connectionInfo, e);
                    Platform.runLater(() -> showAsClosed(e));
                }
                catch (ExecutionException e)
                {
                    logger.log(Level.WARNING,
                        "Unexpected error while reading", e);
                    Platform.runLater(() -> showAsClosed(e));
                }
                catch (InterruptedException e)
                {
                    logger.log(Level.INFO, "Interrupted; exiting", e);
                    Platform.runLater(() -> close());
                }
            }
        };
        new Thread(connectionInitializer, threadName).start();
    }

    private void processEnter(KeyEvent event)
    {
        if (event.getCode() == KeyCode.ENTER
            && !event.isShiftDown() && !event.isControlDown()
            && !event.isAltDown() && !event.isMetaDown())
        {
            event.consume();

            String text = inputField.getText();
            if (text.endsWith("\n"))
            {
                text = text.substring(0, text.length() - 1);
            }

            sendLine(text);

            if (!text.isEmpty())
            {
                history.commit(text);
            }

            Platform.runLater(() -> inputField.setText(""));

            processTriggers(text, connectionInfo.getSendTriggers());
        }
    }

    void sendLine(String rawText)
    {
        sendLine(rawText, 0);
    }

    /**
     * @see <a href="http://unicode.org/reports/tr44/#General_Category_Values">General Category Values</a>
     */
    private static byte[] UNICODE_CONTROL_CATEGORIES = {
        Character.CONTROL,      // Cc
        Character.FORMAT,       // Cf
        Character.SURROGATE,    // Cs
        Character.PRIVATE_USE,  // Co
        Character.UNASSIGNED,   // Cn
    };

    private static boolean isControl(int codepoint)
    {
        int category = Character.getType(codepoint);
        for (byte controlCategory : UNICODE_CONTROL_CATEGORIES)
        {
            if (category == controlCategory)
            {
                return true;
            }
        }

        return false;
    }

    private boolean isPrintable(int codepoint)
    {
        //if (isControl(codepoint))
        //{
        //    return false;
        //}

        if (Character.isSupplementaryCodePoint(codepoint))
        {
            String s = String.valueOf(Character.toChars(codepoint));
            return charsetEncoder.canEncode(s);
        }
        else
        {
            return charsetEncoder.canEncode((char) codepoint);
        }
    }

    private void sendLine(String rawText,
                          long delayInMillis)
    {
        final String text;
        if (rawText != null)
        {
            Formatter encodedText =
                new Formatter(new StringBuilder(rawText.length()));
            rawText.codePoints().forEachOrdered(c -> encodedText.format(
                isPrintable(c) ? "%2$c" : "%sU+%04x%s",
                unicodeStart, c, unicodeEnd));
            text = encodedText.toString();
        }
        else
        {
            text = "";
        }

        Runnable sender = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    logger.log(Level.FINER, "Sending \"{0}\" CR LF", text);
                    connection.write(text + "\r\n");
                }
                catch (IOException e)
                {
                    logger.log(Level.WARNING, "Couldn't send text", e);
                    Platform.runLater(() -> showAsClosed(e));
                }
            }
        };

        sendQueue.schedule(sender, delayInMillis, TimeUnit.MILLISECONDS);
    }

    void previousInHistory()
    {
        history.setCurrent(inputField.getText());

        String text = history.previous();
        if (text != null)
        {
            inputField.setText(text);
        }
    }

    void nextInHistory()
    {
        history.setCurrent(inputField.getText());

        String text = history.next();
        if (text != null)
        {
            inputField.setText(text);
        }
    }

    Tab getTab()
    {
        return tab;
    }

    private void updateScrolledToEnd(Number value)
    {
        //scrolledToEnd = (value.doubleValue() == outputPane.getVmax());
    }

    private void confirmClose(Event event)
    {
        String message = confirmCloseMessageFormat.format(
            new Object[] { connectionInfo.getName() });
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
            ButtonType.YES, ButtonType.NO);
        alert.setTitle(confirmCloseTitle);
        alert.setHeaderText(confirmCloseHeader);

        if (!alert.showAndWait().filter(r -> r == ButtonType.YES).isPresent())
        {
            event.consume();
        }
    }

    void close()
    {
        showAsClosed();
        readExecutor.shutdown();
        sendQueue.shutdown();
        beepTimer.shutdown();

        if (connection != null)
        {
            Runnable closer = new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        connection.close();
                    }
                    catch (IOException e)
                    {
                        logger.log(Level.INFO,
                            "Couldn't close " + connection, e);
                    }
                }
            };
            new Thread(closer).start();
        }
    }

    private void showAsClosed(Throwable t)
    {
        showAsClosed();
        showError(t);
    }

    private void showAsClosed()
    {
        connected.set(false);
        //inputField.setDisable(true);
        String namePart = closedTitleFormat.format(
            new Object[] { connectionInfo.getName() });
        //tab.setText(namePart + closedTitleSuffix);
        tab.setText(null);

        Node statusPart = new Label(closedTitleSuffix);
        statusPart.getStyleClass().add("closed");

        TextFlow tabGraphic = new TextFlow(new Text(namePart), statusPart);
        tabGraphic.getStylesheets().addAll(
            Chat.class.getResource("style.css").toString());

        tab.setGraphic(tabGraphic);
    }

    private void beep()
    {
        // I don't like mixing AWT with JavaFX, but there's currently
        // no other way to play a system beep.
        EventQueue.invokeLater(() -> Toolkit.getDefaultToolkit().beep());
    }

    private void beepRepeatedly(int count)
    {
        for (int i = 0; i < count; i++)
        {
            beepTimer.schedule(this::beep, i, TimeUnit.SECONDS);
        }
    }

    private void appendOutputLater(String chars)
    {
        Platform.runLater(() -> appendOutput(chars));
    }

    private void appendOutput(String chars)
    {
        logger.finest(() -> "Appending output \"" + escape(chars) + "\"" +
            (chars.contains("\n") ? " (has newline)" : " (no newline)"));

        boolean scrolledToEnd = true;
/*
            (Boolean) outputField.executeScript(
                "var b = document.body;" +
                " b.scrollHeight - b.scrollTop == b.clientHeight;");
*/
/*
            outputField.executeScript("window.scrollY + 
                document.body.scrollHeight);");
*/
        logger.finest(() -> "scrolledToEnd=" + scrolledToEnd);

        Document doc = outputField.getDocument();
        Element body = doc.getElementById("body");

        String[] lines = chars.split("\r?\n", -1);
        int lineCount = lines.length;
        for (int i = 0; i < lineCount; i++)
        {
            if (finalLine != null && finalLine.getParentNode() != null)
            {
                finalLine.getParentNode().removeChild(finalLine);
            }

            boolean newLine = false;
            if (i > 0 && lineCount > 1)
            {
                newLine = true;
                body.appendChild(doc.createElement("br"));

                // Placeholder to force WebView to render newline after
                // final <br> element.
                if (finalLine == null)
                {
                    finalLine = doc.createElement("span");
                    //finalLine.setAttribute("id", FINAL_SPAN_ID);
                    finalLine.setAttribute("style", "padding-bottom: 1px;");
                }
                body.appendChild(finalLine);
            }

            String line = lines[i];
            line = replaceUnicode(line);

            //boolean bellFound = replaceTextAttributesWith(line);
            csiMatcher.reset(line);

            boolean bellFound = false;

            textAttributes.clearText();
            String csi = null;
            int csiEnd = 0;
            while (csiMatcher.find())
            {
                String run = line.substring(csiEnd, csiMatcher.start());
                if (bellFound |= line.contains("\007"))
                {
                    run = run.replace("\007", "");
                }

                textAttributes.append(csi, run);
                csi = csiMatcher.group().substring(2);
                csiEnd = csiMatcher.end();
            }
            textAttributes.append(csi, line.substring(csiEnd));
            //

            if (bellFound)
            {
                beep();
            }

            TriggerResult result = null;

            if (!userTriggerTripped)
            {
                String user = connectionInfo.getUser();
                Trigger trigger = connectionInfo.getUserTrigger();
                if (user != null && trigger != null && trigger.matches(line))
                {
                    userTriggerTripped = true;
                    String password = connectionInfo.getPassword();
                    if (connectionInfo.isSendPasswordWithUser() &&
                        password != null)
                    {
                        String sep = connectionInfo.getUserPassSeparator();
                        sendLine(user + sep + password, RESPONSE_DELAY);
                        passwordTriggerTripped = true;
                    }
                    else
                    {
                        logger.fine(() -> "Sending user name \"" + user + "\"");
                        sendLine(user, RESPONSE_DELAY);
                    }
                }
            }
            else if (!passwordTriggerTripped)
            {
                String password = connectionInfo.getPassword();
                Trigger trigger = connectionInfo.getPasswordTrigger();
                if (password != null && trigger != null &&
                    trigger.matches(line))
                {
                    passwordTriggerTripped = true;

                    String l = line;
                    logger.fine(() -> "Sending password in response" +
                        " to line \"" + escape(l) + "\"");
                    sendLine(password, RESPONSE_DELAY);
                }
            }
            else
            {
                result = processTriggers(line, newLine,
                    connectionInfo.getReceiveTriggers());

                if (result.hidden)
                {
                    return;
                }
            }

            //Element lineElement = createLineFromTextAttributes(doc,
            //    Optional.of(result).map(r -> r.timestamp));
            Element lineElement = doc.createElement("span");
            lineElement.setAttribute("class", "line");

            if (result != null && result.timestamp != null)
            {
                lineElement.appendChild(result.timestamp);
            }

            int column = -1;

            AttributedCharacterIterator ci = textAttributes.iterator();
            for (char c = ci.first();
                 c != CharacterIterator.DONE;
                 c = ci.current())
            {
                int start = ci.getRunStart();
                int end = ci.getRunLimit();

                TextAttributes t = TextAttributes.from(ci.getAttributes());

                Object uriAttr = ci.getAttribute(TextAttributes.HYPERLINK);
                if (uriAttr != null)
                {
                    String uri = uriAttr.toString();

                    String style = t.toStyle(true);

                    Element anchor = createElement("a", uri, style, doc);
                    anchor.setAttribute("href", "#");
                    ((EventTarget) anchor).addEventListener("click",
                        e -> uriDisplayer.openURI(uri), false);
                    lineElement.appendChild(anchor);

                    ci.setIndex(end);
                }
                else
                {
                    String text = textAttributes.getText(start, end);
                    ci.setIndex(end);

                    String style = t.toStyle(false);

                    String[] rows = text.split("\r");
                    int numRows = rows.length;
                    for (int r = 0; r < numRows; r++)
                    {
                        if (r > 0)
                        {
                            column = 0;
                        }

                        String row = rows[r];
                        Element span = createElement("span", row, style, doc);
                        if (column < 0)
                        {
                            lineElement.appendChild(span);
                        }
                        else
                        {
                            overwrite(lineElement, span, column);
                            column += row.length();
                        }
                    }
                }
            }

            body.appendChild(lineElement);
        }

        if (scrolledToEnd)
        {
            // Scroll to bottom.
            outputField.executeScript(
                "window.scrollTo(0, document.body.scrollHeight);");
        }
    }

    private void overwrite(Element lineElement,
                           Element span,
                           final int column)
    {
        String lineText = lineElement.getTextContent();
        String spanText = span.getTextContent();
        int spanEnd = column + spanText.length();

        logger.finer(() -> "Overwriting at column " + column +
            " with \"" + escape(spanText) + "\"");

        int lineLen = lineText.length();
        if (column >= lineLen)
        {
            lineElement.appendChild(span);
            return;
        }

        NodeList children = lineElement.getElementsByTagName("*");
        int count = children.getLength();

        int chStart = 0;
        int chEnd;
        for (int i = 0; i < count; i++)
        {
            int elementIndex = i;
            logger.finest(() -> "Checking element " + elementIndex +
                ", line element=" + toXML(lineElement));

            Element child = (Element) children.item(i);
            String text = child.getTextContent();
            chEnd = chStart + text.length();

            if (chStart <= column && column < chEnd)
            {
                child.setTextContent(text.substring(0, column - chStart));
                lineElement.insertBefore(span, child.getNextSibling());

                if (spanEnd <= chEnd)
                {
                    Element tail = (Element) child.cloneNode(false);
                    tail.setTextContent(text.substring(spanEnd - chStart));
                    lineElement.insertBefore(tail, span.getNextSibling());
                    break;
                }
            }
            if (chStart >= column && chEnd < spanEnd)
            {
                lineElement.removeChild(child);
            }
            if (column < chStart && spanEnd <= chEnd)
            {
                child.setTextContent(text.substring(spanEnd - chStart));
                break;
            }

            chStart = chEnd;
            if (chStart >= spanEnd)
            {
                break;
            }
        }
    }

/*
    private boolean replaceTextAttributesWith(String line)
    {
        csiMatcher.reset(line);

        boolean bellFound = false;

        textAttributes.clearText();
        String csi = null;
        int csiEnd = 0;
        while (csiMatcher.find())
        {
            String run = line.substring(csiEnd, csiMatcher.start());
            if (bellFound |= line.contains("\007"))
            {
                run = run.replace("\007", "");
            }

            textAttributes.append(csi, run);
            csi = csiMatcher.group().substring(2);
            csiEnd = csiMatcher.end();
        }
        textAttributes.append(csi, line.substring(csiEnd));

        return bellFound;
    }
*/

/*
    private Element createLineFromTextAttributes(Document doc,
                                                 Optional<Element> timestamp)
    {
        Element lineElement = doc.createElement("span");
        timestamp.ifPresent(e -> lineElement.appendChild(e));

        AttributedCharacterIterator ci = textAttributes.iterator();
        for (char c = ci.first();
             c != CharacterIterator.DONE;
             c = ci.current())
        {
            int start = ci.getRunStart();
            int end = ci.getRunLimit();

            TextAttributes t = TextAttributes.from(ci.getAttributes());

            Object uriAttr = ci.getAttribute(TextAttributes.HYPERLINK);
            if (uriAttr != null)
            {
                String uri = uriAttr.toString();

                String style = t.toStyle(true);

                Element anchor = createElement("a", uri, style, doc);
                anchor.setAttribute("href", "#");
                ((EventTarget) anchor).addEventListener("click",
                    e -> uriDisplayer.openURI(uri), false);
                lineElement.appendChild(anchor);

                ci.setIndex(end);
            }
            else
            {
                String text = textAttributes.getText(start, end);
                ci.setIndex(end);

                String style = t.toStyle(false);

                Element span = createElement("span", text, style, doc);
                lineElement.appendChild(span);
            }
        }

        return lineElement;
    }
*/

    /**
     * Escapes non-ASCII charcters in a string as
     * <code>&#x5c;u</code> sequences, for logging purposes.
     *
     * @param text string to escape
     *
     * @return escaped string
     */
    static String escape(String text)
    {
        Formatter s = new Formatter(new StringBuilder(text.length()));
        int len = text.length();
        for (int i = 0; i < len; i++)
        {
            char c = text.charAt(i);
            if (c < 32 || c >= 127)
            {
                s.format("\\u%04x", (int) c);
            }
            else
            {
                s.format("%c", c);
            }
        }
        return s.toString();
    }

    private static String toXML(org.w3c.dom.Node node)
    {
        Document doc = node.getOwnerDocument();
        LSSerializer serializer = (LSSerializer)
            doc.getImplementation().getFeature("LS", "3.0");
        return serializer.writeToString(node);
    }

    private String replaceUnicode(String text)
    {
        StringBuffer newText = new StringBuffer(text.length());

        Pattern codepointPattern = Pattern.compile(
            Pattern.quote(unicodeStart) +
            "U\\+(\\p{XDigit}+)" +
            Pattern.quote(unicodeEnd));
        Matcher matcher = codepointPattern.matcher(text);
        while (matcher.find())
        {
            int codepoint = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(newText, String.format("%c",
                Character.isValidCodePoint(codepoint) ? codepoint : 0xfffd));
        }
        matcher.appendTail(newText);

        return newText.toString();
    }

    private Element createElement(String name,
                                  String content,
                                  String style,
                                  Document doc)
    {
        Element element = doc.createElement(name);
        element.appendChild(doc.createTextNode(content));

        if (!style.isEmpty())
        {
            element.setAttribute("style", style);
        }

        return element;
    }

    /**
     * @return whether line should be hidden
     */
    private TriggerResult processTriggers(String line,
                                    Iterable<? extends Trigger> triggers)
    {
        return processTriggers(line, false, triggers);
    }

    /**
     * @return whether line should be hidden
     */
    private TriggerResult processTriggers(String line,
                                          boolean newLine,
                                          Iterable<? extends Trigger> triggers)
    {
        TriggerResult result = new TriggerResult();

        Document doc = outputField.getDocument();

        for (Trigger trigger : triggers)
        {
            if (trigger.isOneTime() &&
                oneTimeTriggersTripped.contains(trigger))
            {
                continue;
            }

            Matcher matcher = trigger.toRegex().matcher(line);
            while (matcher.find())
            {
                if (trigger.isOneTime())
                {
                    oneTimeTriggersTripped.add(trigger);
                }

                for (Action action : trigger.getActions())
                {
                    switch (action.getType())
                    {
                        case HIDE:
                            result.hidden = true;
                            break;
                        case TIMESTAMP:
                            if (!newLine)
                            {
                                continue;
                            }
                            String timestamp =
                                String.format(timestampFormat,
                                    action.getTimestampText());

                            String style = toStyle(
                                action.getForeground(),
                                action.getBackground());

                            result.timestamp = createElement("span",
                                timestamp, style, doc);
                            break;
                        case COLOR:
                            int count = matcher.groupCount();
                            int firstGroup = (count > 0 ? 1 : 0);
                            for (int g = firstGroup; g <= count; g++)
                            {
                                textAttributes.setColors(
                                    action.getForeground(),
                                    action.getBackground(),
                                    matcher.start(g),
                                    matcher.end(g));
                            }
                            break;
                        case SEND:
                            for (int j = action.getCount(); j > 0; j--)
                            {
                                sendLine(action.getDetail());
                            }
                            break;
                        case BEEP:
                            beepRepeatedly(action.getCount());
                            break;
                        case SOUND:
                            play(action.getDetail(),
                                 action.getCount());
                            break;
                        default:
                            throw new RuntimeException(
                                "Unknown action type: " + action.getType());
                    }
                }
            }
        }

        return result;
    }

    private void play(String soundFile,
                      int count)
    {
        try
        {
            MediaPlayer player = new MediaPlayer(
                new Media(Paths.get(soundFile).toUri().toString()));
            player.setOnEndOfMedia(player::dispose);
            player.setCycleCount(count);
            player.play();
        }
        catch (MediaException e)
        {
            logger.log(Level.WARNING,
                "Couldn't play \"" + soundFile + "\"", e);
        }
    }

    boolean contains(Node node)
    {
        while (node != null)
        {
            if (node == tab.getContent())
            {
                return true;
            }
            node = node.getParent();
        }

        return false;
    }

    private void showError(Throwable t)
    {
        errorHandler.showError(t, inputField.getScene().getWindow());
    }
}
