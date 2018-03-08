package net.pan.textend;

import java.text.MessageFormat;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import java.util.Map;
import java.util.EnumMap;

import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public class Trigger
{
    private static final Logger logger =
        Logger.getLogger(Trigger.class.getName());

    private static final MessageFormat unclosedGlobClassFormat =
        new MessageFormat("Unclosed character class");

    private final StringProperty name;

    private final StringProperty pattern;

    private final ObjectProperty<PatternType> patternType;

    private final StringProperty comments;

    private final BooleanProperty oneTime;

    private final ObjectProperty<ObservableList<Action>> actions =
        new SimpleObjectProperty<>(FXCollections.observableArrayList());

    public Trigger()
    {
        name = new SimpleStringProperty(this, "name");
        pattern = new SimpleStringProperty(this, "pattern");
        patternType = new SimpleObjectProperty<>(this, "patternType",
            PatternType.TEXT_CASELESS);
        comments = new SimpleStringProperty(this, "comments");
        oneTime = new SimpleBooleanProperty(this, "oneTime");
    }

    @Override
    public String toString()
    {
        return String.format("%s['%s' %s \"%s\"]", getClass().getSimpleName(),
            getName(), getPatternType(), getPattern());
    }

    public Trigger(String name)
    {
        this();
        setName(name);
    }

    public Trigger(String name,
                   String pattern)
    {
        this();
        setName(name);
        setPattern(pattern);
    }

    public Trigger(String name,
                   PatternType type,
                   String pattern)
    {
        this();
        setName(name);
        setPattern(pattern);
        setPatternType(type);
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
        //Objects.requireNonNull(name, "Name cannot be null");
        this.name.set(name);
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
        //Objects.requireNonNull(pattern, "Pattern cannot be null");
        this.pattern.set(pattern);
    }

    public ObjectProperty<PatternType> patternTypeProperty()
    {
        return patternType;
    }

    public PatternType getPatternType()
    {
        return patternType.get();
    }

    public void setPatternType(PatternType type)
    {
        //Objects.requireNonNull(type, "Pattern type cannot be null");
        this.patternType.set(type);
    }

    Pattern toRegex()
    throws PatternSyntaxException
    {
        return patternToRegex(getPattern(), getPatternType(),
            unclosedGlobClassFormat);
    }

    @SuppressWarnings("fallthrough")
    static Pattern patternToRegex(
                            String pattern,
                            PatternType type,
                            MessageFormat unclosedGlobClassMessageFormat)
    throws PatternSyntaxException
    {
        if (pattern == null)
        {
            pattern = "";
        }

        int flags = Pattern.CANON_EQ | Pattern.UNICODE_CHARACTER_CLASS;

        switch (type)
        {
            case REGEX:
                return Pattern.compile(pattern);
            case TEXT_CASELESS:
                flags |= Pattern.CASE_INSENSITIVE;
            case TEXT:
                flags |= Pattern.LITERAL | Pattern.UNICODE_CASE;
                return Pattern.compile(pattern, flags);
            case GLOB_CASELESS:
                flags |= Pattern.CASE_INSENSITIVE;
            case GLOB:
                StringBuilder regex = new StringBuilder();
                //regex.append("^");

                boolean escaped = false;
                CharacterIterator i = new StringCharacterIterator(pattern);
                for (char c = i.first();
                     c != CharacterIterator.DONE;
                     c = i.next())
                {
                    if (escaped || "\\?*[".indexOf(c) < 0)
                    {
                        if ("$^*()+[]{}|\\.?".indexOf(c) >= 0)
                        {
                            regex.append('\\');
                        }
                        regex.append(c);
                        escaped = false;
                    }
                    else if (c == '\\')
                    {
                        escaped = true;
                    }
                    else if (c == '?')
                    {
                        regex.append('.');
                    }
                    else if (c == '*')
                    {
                        regex.append(".*");
                    }
                    else if (c == '[')
                    {
                        int globCharClassStart = i.getIndex();
                        int regexCharClassStart = regex.length();

                        StringBuilder charClass = new StringBuilder("[");

                        while ((c = i.next()) != CharacterIterator.DONE)
                        {
                            if (c == '!' && charClass.length() == 1)
                            {
                                charClass.append("^");
                            }
                            else if (c == '\\')
                            {
                                charClass.append("\\\\");
                            }
                            else if (c == '&')
                            {
                                charClass.append("\\&");
                            }
                            else
                            {
                                charClass.append(c);
                                if (c == ']')
                                {
                                    String s = charClass.toString();
                                    if (!s.equals("[]") && !s.equals("[^]"))
                                    {
                                        break;
                                    }
                                }
                            }
                        }

                        regex.append(charClass);

                        if (c == CharacterIterator.DONE)
                        {
                            String originalGlobCharClass =
                                pattern.substring(globCharClassStart);
                            throw new PatternSyntaxException(
                                unclosedGlobClassMessageFormat.format(
                                    new Object[] { originalGlobCharClass }),
                                regex.toString(), regexCharClassStart);
                        }
                    }
                }
                //regex.append("$");
                return Pattern.compile(regex.toString(), flags);
            default:
                throw new IllegalArgumentException(
                    "Unknown pattern type: " + type);
        }
    }

    public boolean matches(CharSequence text)
    {
        logger.finer(() -> "regex=\"" + toRegex() + "\"");
        return toRegex().matcher(text).find();
    }

    public StringProperty commentsProperty()
    {
        return comments;
    }

    public String getComments()
    {
        return comments.get();
    }

    public void setComments(String comments)
    {
        this.comments.set(comments);
    }

    public BooleanProperty oneTimeProperty()
    {
        return oneTime;
    }

    public boolean isOneTime()
    {
        return oneTime.get();
    }

    public void setOneTime(boolean oneTime)
    {
        this.oneTime.set(oneTime);
    }

    public ObjectProperty<ObservableList<Action>> actionsProperty()
    {
        return actions;
    }

    public ObservableList<Action> getActions()
    {
        return actions.get();
    }

    public void setActions(ObservableList<Action> actions)
    {
        this.actions.set(actions);
    }

    public Action[] getActionsAsArray()
    {
        return getActions().toArray(new Action[0]);
    }

    public void setActionsAsArray(Action[] actions)
    {
        if (actions == null)
        {
            getActions().clear();
        }
        else
        {
            getActions().setAll(actions);
        }
    }
}
