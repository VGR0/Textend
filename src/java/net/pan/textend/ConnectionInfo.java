package net.pan.textend;

import java.nio.charset.StandardCharsets;

import java.util.EnumSet;
import java.util.EnumMap;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

public class ConnectionInfo
{
    private final StringProperty name;

    private final StringProperty host;

    private final IntegerProperty port;

    private final StringProperty username;

    private final StringProperty password;

    private final BooleanProperty ssl;

    private final ObjectProperty<SSLProtocol> sslProtocol;

    private final BooleanProperty requireValidCert;

    private final StringProperty charset;

    private final ObjectProperty<Trigger> usernameTrigger;

    private final ObjectProperty<Trigger> passwordTrigger;

    private final StringProperty userPassSeparator;

    private final BooleanProperty sendPasswordWithUser;

    private final ObservableList<Trigger> receiveTriggers =
        FXCollections.observableArrayList();

    private final ObservableList<Trigger> sendTriggers =
        FXCollections.observableArrayList();

    private final ObservableMap<TriggerType, ObservableList<Trigger>>
        allTriggers = FXCollections.observableMap(
            new EnumMap<TriggerType, ObservableList<Trigger>>(
                TriggerType.class));
    {
        allTriggers.put(TriggerType.RECEIVE, receiveTriggers);
        allTriggers.put(TriggerType.SEND, sendTriggers);

        if (!allTriggers.keySet().containsAll(
            EnumSet.allOf(TriggerType.class)))
        {
            throw new RuntimeException(
                "Not all TriggerType values have Trigger lists");
        }
    }

    private final BooleanProperty useSystemColors;

    private final ObjectProperty<Color> foreground;

    private final ObjectProperty<Color> background;

    public ConnectionInfo()
    {
        name = new SimpleStringProperty(this, "name");
        host = new SimpleStringProperty(this, "host");
        port = new SimpleIntegerProperty(this, "port", 23);
        username = new SimpleStringProperty(this, "user");
        password = new SimpleStringProperty(this, "password");
        ssl = new SimpleBooleanProperty(this, "SSL");
        sslProtocol = new SimpleObjectProperty<>(this, "SSLProtocol",
            SSLProtocol.defaultProtocol());
        requireValidCert =
            new SimpleBooleanProperty(this, "requireValidCertificate", true);
        charset = new SimpleStringProperty(this, "charset",
            StandardCharsets.US_ASCII.name());

        // These two Triggers have names that aren't meaningful, as they're
        // never seen by the user.  The only purpose of the Trigger names is
        // for debugging.

        usernameTrigger =
            new SimpleObjectProperty<>(this, "userTrigger",
                new Trigger("user",
                    PatternType.GLOB_CASELESS, "User*:"));
                    //PatternType.REGEX, "User( ?[Nn]ame)?:? *$"));
        passwordTrigger =
            new SimpleObjectProperty<>(this, "passwordTrigger",
                new Trigger("password",
                    PatternType.GLOB_CASELESS, "Password:"));
                    //PatternType.REGEX, "Password:? *$"));

        userPassSeparator =
            new SimpleStringProperty(this, "userPassSeparator", " ");
        sendPasswordWithUser =
            new SimpleBooleanProperty(this, "sendPasswordWithUser");

        useSystemColors =
            new SimpleBooleanProperty(this, "useSystemColors", true);
        foreground =
            new SimpleObjectProperty<>(this, "foreground", Color.BLACK);
        background =
            new SimpleObjectProperty<>(this, "background", Color.WHITE);
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    public String getName()
    {
        return name.get();
    }

    public void setName(String name)
    {
        this.name.set(name);
    }

    public StringProperty hostProperty()
    {
        return host;
    }

    public String getHost()
    {
        return host.get();
    }

    public void setHost(String host)
    {
        this.host.set(host);
    }

    public IntegerProperty portProperty()
    {
        return port;
    }

    public int getPort()
    {
        return port.get();
    }

    public void setPort(int port)
    {
        this.port.set(port);
    }

    public StringProperty userProperty()
    {
        return username;
    }

    public String getUser()
    {
        return username.get();
    }

    public void setUser(String user)
    {
        this.username.set(user);
    }

    public StringProperty passwordProperty()
    {
        return password;
    }

    public String getPassword()
    {
        return password.get();
    }

    public void setPassword(String password)
    {
        this.password.set(password);
    }

    /**
     * Whether connection is an SSL connection.  Default value is false.
     *
     * @return property representing whether connection is SSL connection
     *
     * @see #requireValidCertificateProperty()
     */
    public BooleanProperty SSLProperty()
    {
        return ssl;
    }

    public boolean isSSL()
    {
        return ssl.get();
    }

    public void setSSL(boolean ssl)
    {
        this.ssl.set(ssl);
    }

    public ObjectProperty<SSLProtocol> SSLProtocolProperty()
    {
        return sslProtocol;
    }

    public SSLProtocol getSSLProtocol()
    {
        return sslProtocol.get();
    }

    public void setSSLProtocol(SSLProtocol protocol)
    {
        this.sslProtocol.set(protocol);
    }

    /**
     * Whether SSL connection requires a valid certificate.
     * Only meaningful if {@link #SSLProperty()} returns true.
     * Default value is true.
     *
     * @return property representing whether SSL connection requires a
     *         valid certificate
     */
    public BooleanProperty requireValidCertificateProperty()
    {
        return requireValidCert;
    }

    public boolean getRequireValidCertificate()
    {
        return requireValidCert.get();
    }

    public void setRequireValidCertificate(boolean require)
    {
        this.requireValidCert.set(require);
    }

    public StringProperty charsetProperty()
    {
        return charset;
    }

    public String getCharset()
    {
        return charset.get();
    }

    public void setCharset(String charset)
    {
        this.charset.set(charset);
    }

    /**
     * One-time trigger which determines when, during the login process,
     * to send this connection's user name.  The {@code Trigger}'s actions
     * are ignored.  If the value is {@code null} or empty, no automatic login
     * takes place.
     *
     * @return property containing (possibly {@code null}) user name trigger
     */
    public ObjectProperty<Trigger> userTriggerProperty()
    {
        return usernameTrigger;
    }

    public Trigger getUserTrigger()
    {
        return usernameTrigger.get();
    }

    public void setUserTrigger(Trigger trigger)
    {
        this.usernameTrigger.set(trigger);
    }

    /**
     * One-time trigger which determines when, during the login process,
     * to send this connection's password.  The {@code Trigger}'s actions
     * are ignored.  If the value is {@code null} or empty, the password is
     * sent on the same line as the user name, with the value of
     * {@link #userPassSeparatorProperty()} between them (that is,
     * {@code user + userPassSeparator + password}).
     *
     * @return property containing (possibly {@code null}) login password
     *         trigger
     */
    public ObjectProperty<Trigger> passwordTriggerProperty()
    {
        return passwordTrigger;
    }

    public Trigger getPasswordTrigger()
    {
        return passwordTrigger.get();
    }

    public void setPasswordTrigger(Trigger trigger)
    {
        this.passwordTrigger.set(trigger);
    }

    /**
     * The text sent between the user and password, if they are both sent
     * in one line.  Only meaningful if value of
     * {@link #passwordTriggerProperty()} is {@code null}.
     *
     * @return property containing text to send between user and password
     */
    public StringProperty userPassSeparatorProperty()
    {
        return userPassSeparator;
    }

    public String getUserPassSeparator()
    {
        return userPassSeparator.get();
    }

    public void setUserPassSeparator(String separator)
    {
        this.userPassSeparator.set(separator);
    }

    public BooleanProperty sendPasswordWithUserProperty()
    {
        return sendPasswordWithUser;
    }

    public boolean isSendPasswordWithUser()
    {
        return sendPasswordWithUser.get();
    }

    public void setSendPasswordWithUser(boolean sendWithUser)
    {
        this.sendPasswordWithUser.set(sendWithUser);
    }

    public ObservableMap<TriggerType, ObservableList<Trigger>> getAllTriggers()
    {
        return allTriggers;
    }

    public ObservableList<Trigger> getReceiveTriggers()
    {
        return receiveTriggers;
    }

    public Trigger[] getReceiveTriggersAsArray()
    {
        return receiveTriggers.toArray(new Trigger[0]);
    }

    public void setReceiveTriggersAsArray(Trigger[] triggers)
    {
        if (triggers == null)
        {
            receiveTriggers.clear();
        }
        else
        {
            receiveTriggers.setAll(triggers);
        }
    }

    public ObservableList<Trigger> getSendTriggers()
    {
        return sendTriggers;
    }

    public Trigger[] getSendTriggersAsArray()
    {
        return sendTriggers.toArray(new Trigger[0]);
    }

    public void setSendTriggersAsArray(Trigger[] triggers)
    {
        if (triggers == null)
        {
            sendTriggers.clear();
        }
        else
        {
            sendTriggers.setAll(triggers);
        }
    }

    public BooleanProperty useSystemColorsProperty()
    {
        return useSystemColors;
    }

    public boolean isUseSystemColors()
    {
        return useSystemColors.get();
    }

    public void setUseSystemColors(boolean use)
    {
        this.useSystemColors.set(use);
    }

    public ObjectProperty<Color> foregroundProperty()
    {
        return foreground;
    }

    public Color getForeground()
    {
        return foreground.get();
    }

    public void setForeground(Color color)
    {
        this.foreground.set(color);
    }

    public ObjectProperty<Color> backgroundProperty()
    {
        return background;
    }

    public Color getBackground()
    {
        return background.get();
    }

    public void setBackground(Color color)
    {
        this.background.set(color);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName()
            + "[" + getName() + ": " + getHost() + " " + getPort() + "]";
    }
}
