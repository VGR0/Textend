package net.pan.textend;

import java.text.MessageFormat;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ResourceBundle;
import java.util.Objects;

import java.util.function.Function;

import javafx.stage.Window;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

class ErrorHandler
{
    private final String title;
    private final String header;
    private final Function<Throwable, String> messageFactory;

    ErrorHandler(String title,
                 String header,
                 Function<Throwable, String> messageFactory)
    {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.header = Objects.requireNonNull(header, "Header cannot be null");
        this.messageFactory = Objects.requireNonNull(messageFactory,
            "Message supplier cannot be null");
    }

    static ErrorHandler createNetworkErrorInstance(
                                      ConnectionInfo connectionInfo)
    {
        ResourceBundle res = ResourceBundle.getBundle(
            ErrorHandler.class.getPackage().getName() + ".Localization");

        String title = res.getString("networkError.title");
        String header = res.getString("networkError.header");

        MessageFormat messageFormat =
            new MessageFormat(res.getString("networkError.message"));

        String host = connectionInfo.getHost();
        // Using String.valueOf because MessageFormat will localize numbers.
        String port = String.valueOf(connectionInfo.getPort());

        return new ErrorHandler(title, header,
            t -> messageFormat.format(new Object[] { host, port, t }));
    }

    void showError(Throwable t,
                   Window parent)
    {
        StringWriter stackTrace = new StringWriter();
        t.printStackTrace(new PrintWriter(stackTrace));
        TextArea stackTraceNode = new TextArea(stackTrace.toString());
        stackTraceNode.setEditable(false);

        String message = messageFactory.apply(t);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(parent);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getDialogPane().setExpandableContent(stackTraceNode);

        alert.show();
    }
}
