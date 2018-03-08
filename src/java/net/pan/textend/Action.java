package net.pan.textend;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import java.util.Map;
import java.util.EnumMap;

import java.util.Collections;
import java.util.Objects;
import java.util.ResourceBundle;

import java.util.stream.Stream;

//import java.util.logging.Logger;
//import java.util.logging.Level;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

/**
 * Response to a {@link Trigger}.
 */
public class Action
{
    /*
     * There is a reason this class does use polymorphism and subclassing:
     * JavaFX properties cannot be bound to a property if the current Action
     * is a subclass that doesn't have that property.  So this class contains
     * all properties for all possible types of actions.
     */

    /**
     * All {@linkplain Action.Type action types} belong to exactly one category.
     */
    public enum Category
    {
        /**
         * Modification of incoming text.
         */
        MODIFICATION,
        /**
         * Response to a match.
         */
        RESPONSE;
    }

    /**
     * Describes what operation(s) an {@link Action} represents.
     */
    public enum Type
    {
        /**
         * Display triggering text in a particular color.
         */
        COLOR(Category.MODIFICATION),
        /**
         * Do not show triggering text in the UI at all.
         */
        HIDE(Category.MODIFICATION),
        /**
         * Prefix triggering text with date and/or time.
         * Detail is {@code DateTimeFormatter} format string.
         */
        TIMESTAMP(Category.MODIFICATION),
        /**
         * Send specific text in response to having received triggering text.
         * Detail is lines to send.
         */
        SEND(Category.RESPONSE),
        /**
         * Execute one or more system beeps.
         */
        BEEP(Category.RESPONSE),
        /**
         * Detail is path of file to play.
         */
        SOUND(Category.RESPONSE);

        /**
         * Non-{@code null} category of this action type.
         */
        final Category category;

        private Type(Category category)
        {
            this.category = Objects.requireNonNull(category,
                "Category cannot be null");
        }
    }

    /**
     * printf format string for ARGB values of color (in that order).
     */
    private static final String rgbFormat = "#%02x%02x%02x%02x";

    /**
     * Describes what operation(s) this {@link Action} represents.
     */
    private final ObjectProperty<Type> type =
        new SimpleObjectProperty<>(Type.SEND);

    /**
     * One-line human-readable description of this action.  Intended to be
     * displayed in a table column.
     */
    private final ReadOnlyStringWrapper summary =
        new ReadOnlyStringWrapper();

    /**
     * Interpretation of this value depends on the {@link #type};  if this
     * action requires specific text, this property contains that text.
     */
    private final StringProperty detail = new SimpleStringProperty();

    /**
     * How long to wait between repetitions, in milliseconds,
     * if {@link #count} is more than 1.
     */
    private final IntegerProperty delay = new SimpleIntegerProperty();

    /**
     * Number of times to repeat this action's operation (beeping, sending
     * of text, etc.  Meaningless for actions which modify triggering text.
     */
    private final IntegerProperty count = new SimpleIntegerProperty(1);

    /**
     * Whether {@linkplain #foreground text color} should be changed;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     */
    private final BooleanProperty foregroundSpecified =
        new SimpleBooleanProperty(true);

    /**
     * Text color;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     */
    private final ObjectProperty<Color> foreground =
        new SimpleObjectProperty<>(Color.YELLOW);

    /**
     * Whether {@linkplain #background text background} should be changed;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     */
    private final BooleanProperty backgroundSpecified =
        new SimpleBooleanProperty(false);

    /**
     * Text background color;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     */
    private final ObjectProperty<Color> background =
        new SimpleObjectProperty<>(Color.BLACK);

    /**
     * Which Java {@code FormatStyle} to use for times in timestamps, or
     * {@link FormatType#NONE} to show date only;
     * for {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     * This must not be {@code null} (since JavaFX controls are bound to it).
     */
    private final ObjectProperty<FormatType> timeStyle =
        new SimpleObjectProperty<>(FormatType.SHORT);

    /**
     * Which Java {@code FormatStyle} to use for dates in timestamps, or
     * {@link FormatType#NONE} to show time only;
     * for {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     * This must not be {@code null} (since JavaFX controls are bound to it).
     */
    private final ObjectProperty<FormatType> dateStyle =
        new SimpleObjectProperty<>(FormatType.NONE);

    /** Arguments: foreground, background */
    private final String colorSummaryFormat;
    /** Arguments: background */
    private final String backgroundSummaryFormat;
    /** Arguments: Date style, time style */
    private final String dateTimeStyleFormat;

    /**
     * Human-readable names for {@code FormatType} constants.
     */
    private final Map<FormatType, String> styleNames;

    //private final Map<Type, StringExpression> summaries;

    public Action()
    {
        ResourceBundle res = ResourceBundle.getBundle(
            Action.class.getPackage().getName() + ".Localization");

        colorSummaryFormat = res.getString("actions.color.summary.both");
        backgroundSummaryFormat =
            res.getString("actions.color.summary.background");
        dateTimeStyleFormat = 
            res.getString("actions.timestamp.summary");
        styleNames = EnumListCell.getNames(FormatType.class, res);

        Map<Type, StringExpression> summaries = summariesByType();
        summary.bind(Bindings.createStringBinding(
            () -> summaries.get(getType()).getValue(),
            Stream.concat(
                Stream.of(type), summaries.values().stream()).distinct()
                    .toArray(Observable[]::new)));
    }

    /**
     * Generates human-readable name bindings for all action types.
     */
    private Map<Type, StringExpression> summariesByType()
    {
        Map<Type, StringExpression> summaries = new EnumMap<>(Type.class);
        for (Type t : Type.values())
        {
            summaries.put(t, detail);
        }
        // Every type's summary is the same as the detail, except these...
        summaries.put(Type.COLOR,
            Bindings.createStringBinding(this::formatColors,
                foreground, foregroundSpecified,
                background, backgroundSpecified));
        summaries.put(Type.TIMESTAMP,
            Bindings.createStringBinding(this::formatTimestampStyle,
                timeStyle, dateStyle));

        return summaries;
    }

    /**
     * Returns human-readable one-line summary of this action's
     * date and time styles.  Intended for display in a table column.
     */
    private String formatTimestampStyle()
    {
        FormatType t = timeStyle.get();
        FormatType d = dateStyle.get();

        if (t == FormatType.NONE)
        {
            return getDetail();
        }

        return (d == FormatType.NONE ? styleNames.get(t) :
            String.format(dateTimeStyleFormat,
                styleNames.get(d), styleNames.get(t)));
    }

    /**
     * Generates a human-readable one-line summary of this action's foreground
     * and background.  Intended for display in a table column.
     */
    private String formatColors()
    {
        boolean hasForeground = isForegroundSpecified();
        boolean hasBackground = isBackgroundSpecified();

        Color fg = getForeground();
        Color bg = getBackground();

        if (hasForeground && hasBackground)
        {
            return String.format(colorSummaryFormat,
                format(fg), format(bg));
        }
        else if (hasForeground)
        {
            return format(fg);
        }
        else if (hasBackground)
        {
            return String.format(backgroundSummaryFormat, format(bg));
        }
        else
        {
            return null;
        }
    }

    /**
     * Generates one-line summary of a non-{@code null} color.
     */
    private static String format(Color color)
    {
        return String.format(rgbFormat,
            (int) (color.getOpacity() * 255),
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    /**
     * Returns a diagnostic summary of this object.
     */
    @Override
    public String toString()
    {
        return getClass().getName()
            + "[" + getType() + " \"" + getSummary() + "\"]";
    }

    /**
     * Describes what operation(s) this {@link Action} represents.
     *
     * @return property containing this action's operation
     */
    public ObjectProperty<Type> typeProperty()
    {
        return type;
    }

    public Type getType()
    {
        return type.get();
    }

    public void setType(Type type)
    {
        this.type.set(type);
    }

    /**
     * One-line human-readable description of this action.  Intended to be
     * displayed in a table column.
     *
     * @return property containing this action's summary
     */
    public ReadOnlyStringProperty summaryProperty()
    {
        return summary;
    }

    public String getSummary()
    {
        return summary.get();
    }

    /**
     * Delay in milliseconds between repeated operations (such as playing a
     * sound, sending text, etc.) if {@link #countProperty()} is larger than 1.
     * Zero or negative value indicates no delay.
     *
     * @return property containing this action's delay in milliseconds
     */
    public IntegerProperty delayProperty()
    {
        return delay;
    }

    public int getDelay()
    {
        return delay.get();
    }

    public void setDelay(int millis)
    {
        this.delay.set(millis);
    }

    /**
     * Interpretation of this value depends on the {@link #typeProperty type};
     * if this action requires specific text, this property contains that text.
     * <p>
     * Uses of this property by action type:
     * <dl>
     * <dt>{@link Action.Type#TIMESTAMP}
     * <dd>Detail is {@code DateTimeFormatter} format string;  used only if
     *     {@link #timeStyleProperty() time style} is {@code null}
     * <dt>{@link Action.Type#SEND}
     * <dd>Detail is lines to send.
     * <dt>{@link Action.Type#SOUND}
     * <dd>Detail is path of file to play.
     * </dl>
     *
     * @return property containing this action's detail text
     */
    public StringProperty detailProperty()
    {
        return detail;
    }

    public String getDetail()
    {
        return detail.get();
    }

    public void setDetail(String detail)
    {
        this.detail.set(detail);
    }

    /**
     * Number of times to repeat this action's operation (beeping, sending
     * of text, etc.  Meaningless for actions which modify triggering text.
     *
     * @return property containing number of times to apply this action's
     *         operation
     */
    public IntegerProperty countProperty()
    {
        return count;
    }

    public int getCount()
    {
        return count.get();
    }

    public void setCount(int count)
    {
        this.count.set(count);
    }

    /**
     * Whether {@linkplain #foregroundProperty() text color} should be changed;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     *
     * @return property indicating whether text color should change
     */
    public BooleanProperty foregroundSpecifiedProperty()
    {
        return foregroundSpecified;
    }

    public boolean isForegroundSpecified()
    {
        return foregroundSpecified.get();
    }

    public void setForegroundSpecified(boolean specified)
    {
        this.foregroundSpecified.set(specified);
    }

    /**
     * Text color;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     *
     * @return color of text affected or supplied by this action
     */
    public ObjectProperty<Color> foregroundProperty()
    {
        return foreground;
    }

    public Color getForeground()
    {
        return foreground.get();
    }

    public void setForeground(Color foreground)
    {
        this.foreground.set(foreground);
    }

    /**
     * Whether {@linkplain #backgroundProperty() text background} should be
     * changed;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     *
     * @return property indicating whether text background should change
     */
    public BooleanProperty backgroundSpecifiedProperty()
    {
        return backgroundSpecified;
    }

    public boolean isBackgroundSpecified()
    {
        return backgroundSpecified.get();
    }

    public void setBackgroundSpecified(boolean specified)
    {
        this.backgroundSpecified.set(specified);
    }

    /**
     * Whether {@linkplain #backgroundProperty() text background} should be
     * changed;
     * for {@link Action.Type#COLOR Type.COLOR}
     * and {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     *
     * @return property indicating whether text background should change
     */
    public ObjectProperty<Color> backgroundProperty()
    {
        return background;
    }

    public Color getBackground()
    {
        return background.get();
    }

    public void setBackground(Color background)
    {
        this.background.set(background);
    }

    /**
     * Which Java {@code FormatStyle} to use for times in timestamps, or
     * {@link FormatType#NONE} to show date only;
     * for {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     * If {@code null}, {@link #detailProperty() detail} is used as a
     * {@code DateTimeFormatter} format String.
     *
     * @return property indicating format of time in timestamps
     */
    public ObjectProperty<FormatType> timeStyleProperty()
    {
        return timeStyle;
    }

    public FormatType getTimeStyle()
    {
        return timeStyle.get();
    }

    public void setTimeStyle(FormatType style)
    {
        timeStyle.set(style);
    }

    /**
     * Which Java {@code FormatStyle} to use for dates in timestamps, or
     * {@link FormatType#NONE} to show time only;
     * for {@link Action.Type#TIMESTAMP Type.TIMESTAMP} only.
     * This should never be {@code null} for {@code TIMESTAMP} actions.
     *
     * @return property indicating format of dates in timestamps
     */
    public ObjectProperty<FormatType> dateStyleProperty()
    {
        return dateStyle;
    }

    public FormatType getDateStyle()
    {
        return dateStyle.get();
    }

    public void setDateStyle(FormatType style)
    {
        dateStyle.set(style);
    }

    /**
     * Formats the current date and time using this action's date and time
     * styles.  If both styles are {@link FormatType#NONE}, this action's
     * {@link #detailProperty() detail} is used as a {@code DateTimeFormatter}
     * format string.
     *
     * @return non-{@code null} human-readable text of timestamp for
     *         current time
     */
    String getTimestampText()
    {
        FormatStyle t = getTimeStyle().style;
        FormatStyle d = getDateStyle().style;

        DateTimeFormatter formatter;
        if (t != null && d != null)
        {
            formatter = DateTimeFormatter.ofLocalizedDateTime(d, t);
        }
        else if (t != null)
        {
            formatter = DateTimeFormatter.ofLocalizedTime(t);
        }
        else
        {
            formatter = DateTimeFormatter.ofPattern(getDetail());
        }

        return formatter.format(LocalDateTime.now());
    }
}
