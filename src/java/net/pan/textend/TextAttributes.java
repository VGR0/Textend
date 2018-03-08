package net.pan.textend;

import java.text.AttributedString;
import java.text.AttributedCharacterIterator;

import java.awt.font.TextAttribute;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Formatter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.scene.paint.Color;

/**
 * Essentially an {@code AttributedString} with mutable text.
 * Maintains "current" attributes for appending new text
 */
class TextAttributes
{
    private static final Logger logger =
        Logger.getLogger(TextAttributes.class.getName());

    private static final class Attribute
    extends AttributedCharacterIterator.Attribute
    {
        private static final long serialVersionUID = 1;

        private Attribute(String name)
        {
            super(name);
        }
    }

    private enum Enclosure
    {
        FRAMED,
        CIRCLED
    }

    static final AttributedCharacterIterator.Attribute HYPERLINK =
        new Attribute("HYPERLINK");

    private static final AttributedCharacterIterator.Attribute OVERLINE =
        new Attribute("OVERLINE");

    private static final AttributedCharacterIterator.Attribute BLINK =
        new Attribute("BLINK");

    private static final AttributedCharacterIterator.Attribute HIDDEN =
        new Attribute("HIDDEN");

    private static final AttributedCharacterIterator.Attribute ENCLOSURE =
        new Attribute("ENCLOSURE");

    private static final ANSIPalette colors =
        ANSIPalette.get(ANSIPalette.StandardPalette.DEFAULT);

    private static class TextRun
    {
        final int start;
        final int end;
        final Map<AttributedCharacterIterator.Attribute, Object> attributes;

        TextRun(int start,
                int end,
                Map<AttributedCharacterIterator.Attribute, Object> attributes)
        {
            this.start = start;
            this.end = end;
            this.attributes = attributes;
        }
    }

    private static final Pattern rgbCSI = Pattern.compile(
        "(?:" +
            "2;([0-9]{1,3});([0-9]{1,3});([0-9]{1,3})" +
            "|" +
            "5;([0-9]{1,3})" +
        ")" +
        "(;|$)");

    private final Matcher uriMatcher =
        Pattern.compile("(https?|s?ftp)://["
            + "-"                   // mark (RFC 2396)
            + "_.!~*'()"            // mark (RFC 2396)
            + "a-zA-Z0-9"           // alphanum (RFC 2396)
            + ";/?:@&=+$,\\[\\]"    // reserved (RFC 2732)
            + "%"
            + "]+").matcher("");

    private final List<TextRun> runs = new ArrayList<>();

    private final StringBuilder text = new StringBuilder();

    private Color color;

    private Color background;

    private boolean underline;

    private boolean bold;

    private boolean italic;

    private boolean fraktur;

    private boolean inverse;

    private boolean strikethrough;

    private boolean framed;

    private Enclosure enclosure;

    private boolean overline;

    private boolean hidden;

    private boolean blink;

    private boolean blinkEnabled;

    private void reset()
    {
        color = null;
        background = null;
        underline = false;
        bold = false;
        italic = false;
        inverse = false;
        strikethrough = false;
        enclosure = null;
        overline = false;
        hidden = false;
        blink = false;
    }

    static TextAttributes from(
        Map<AttributedCharacterIterator.Attribute, ?> attributes)
    {
        TextAttributes t = new TextAttributes();

        java.awt.Color color;
        float[] rgba = new float[4];

        color = (java.awt.Color) attributes.get(TextAttribute.FOREGROUND);
        if (color != null)
        {
            color.getRGBComponents(rgba);
            t.color = Color.color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        color = (java.awt.Color) attributes.get(TextAttribute.BACKGROUND);
        if (color != null)
        {
            color.getRGBComponents(rgba);
            t.background = Color.color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        Object underline = attributes.get(TextAttribute.UNDERLINE);
        t.underline = TextAttribute.UNDERLINE_ON.equals(underline);

        Object bold = attributes.get(TextAttribute.WEIGHT);
        t.bold = (bold instanceof Number &&
            ((Number) bold).floatValue() >= TextAttribute.WEIGHT_BOLD);

        Object italic = attributes.get(TextAttribute.POSTURE);
        t.italic = TextAttribute.POSTURE_OBLIQUE.equals(italic);

        Object inverse = attributes.get(TextAttribute.SWAP_COLORS);
        t.inverse = Boolean.TRUE.equals(inverse);

        Object strikethrough = attributes.get(TextAttribute.STRIKETHROUGH);
        t.strikethrough = Boolean.TRUE.equals(strikethrough);

        Object enclosure = attributes.get(ENCLOSURE);
        if (enclosure instanceof Enclosure)
        {
            t.enclosure = (Enclosure) enclosure;
        }

        Object overline = attributes.get(OVERLINE);
        t.overline = Boolean.TRUE.equals(overline);

        Object hidden = attributes.get(HIDDEN);
        t.hidden = Boolean.TRUE.equals(hidden);

        Object blink = attributes.get(BLINK);
        t.blink = Boolean.TRUE.equals(blink);

        //t.blinkEnabled = ;

        return t;
    }

    AttributedCharacterIterator iterator()
    {
        logger.finest(() -> "Iterating over \"" + text + "\"");

        AttributedString s = new AttributedString(text.toString());
        for (TextRun run : runs)
        {
            s.addAttributes(run.attributes, run.start, run.end);
        }

        uriMatcher.reset(text);
        while (uriMatcher.find())
        {
            String uri = uriMatcher.group();
            s.addAttribute(HYPERLINK, uri,
                uriMatcher.start(), uriMatcher.end());
        }

        return s.getIterator();
    }

    String getText(int start,
                   int end)
    {
        return text.substring(start, end);
    }

    void clearText()
    {
        text.setLength(0);
        runs.clear();
    }

    /**
     * Appends with current attributes.
     *
     * @param csi ANSI escape sequence, not including the
     *            initial "ESC {@code [}"
     * @param newText text to append using the attributes
     *            specified by {@code csi}
     */
    void append(String csi,
                Object newText)
    {
        if (csi != null)
        {
            updateFromCSI(csi);
        }

        if (newText.toString().isEmpty())
        {
            return;
        }

        int oldLength = text.length();
        text.append(newText);
        runs.add(new TextRun(oldLength, text.length(), toAttributes()));
    }

    void setColors(Color foreground,
                   Color background,
                   int start,
                   int end)
    {
        Map<AttributedCharacterIterator.Attribute, Object> attr =
            new HashMap<>();

        if (foreground != null)
        {
            this.color = foreground;
            attr.put(TextAttribute.FOREGROUND, toAWTColor(foreground));
        }
        if (background != null)
        {
            this.background = background;
            attr.put(TextAttribute.BACKGROUND, toAWTColor(background));
        }

        if (end > start)
        {
            runs.add(new TextRun(start, end, attr));
        }
    }

    private static java.awt.Color toAWTColor(Color color)
    {
        return new java.awt.Color(
            (float) color.getRed(),
            (float) color.getGreen(),
            (float) color.getBlue(),
            (float) color.getOpacity());
    }

    private Map<AttributedCharacterIterator.Attribute, Object> toAttributes()
    {
        Map<AttributedCharacterIterator.Attribute, Object> attr =
            new HashMap<>();

        if (color != null)
        {
            attr.put(TextAttribute.FOREGROUND, toAWTColor(color));
        }
        if (background != null)
        {
            attr.put(TextAttribute.BACKGROUND, toAWTColor(background));
        }
        if (underline)
        {
            attr.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }
        if (bold)
        {
            attr.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        }
        if (italic)
        {
            attr.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        }
        if (strikethrough)
        {
            attr.put(TextAttribute.STRIKETHROUGH, true);
        }
        if (inverse)
        {
            attr.put(TextAttribute.SWAP_COLORS, true);
        }
        if (enclosure != null)
        {
            attr.put(ENCLOSURE, enclosure);
        }
        if (overline)
        {
            attr.put(OVERLINE, true);
        }
        if (hidden)
        {
            attr.put(HIDDEN, true);
        }
        if (blink)
        {
            attr.put(BLINK, true);
        }

        return attr;
    }

    private static void addStyle(Color color,
                                 String property,
                                 Formatter style)
    {
        double red = color.getRed() * 100;
        double green = color.getGreen() * 100;
        double blue = color.getBlue() * 100;
        double alpha = color.getOpacity() * 100;

        style.format("%s: rgb(%s%%,%s%%,%s%%);"
                  + " %s: rgba(%s%%,%s%%,%s%%,%s%%);",
            property, red, green, blue,
            property, red, green, blue, alpha);
    }

    String toStyle(boolean isLink)
    {
        Formatter style = new Formatter();

        if (!isLink && color != null)
        {
            addStyle(color,
                inverse ? "background-color" : "color",
                style);
        }
        if (background != null)
        {
            addStyle(background,
                inverse && !isLink ? "color" : "background-color",
                style);
        }
        if (underline || overline || strikethrough || (blink && blinkEnabled))
        {
            style.format("text-decoration:");
            if (underline)
            {
                style.format(" underline");
            }
            if (overline)
            {
                style.format(" overline");
            }
            if (strikethrough)
            {
                style.format(" line-through");
            }
            if (blink && blinkEnabled)
            {
                style.format(" blink");
            }
            style.format(";");
        }
        if (bold)
        {
            style.format("font-weight: bolder;");
        }
        if (italic)
        {
            style.format("font-style: italic;");
        }
        if (enclosure != null)
        {
            switch (enclosure)
            {
                case FRAMED:
                    style.format("border: solid thin;");
                    break;
                case CIRCLED:
                    style.format("border: solid thin;border-radius: 0.4em;");
                    break;
                default:
                    logger.warning(
                        () -> "Ignoring unknown enclosure value " + enclosure);
                    break;
            }
        }
        if (hidden)
        {
            style.format("display: none;");
        }
        return style.toString();
    }

    @SuppressWarnings("fallthrough")
    private void updateFromCSI(String csi)
    {
        if (!csi.endsWith("m"))
        {
            return;
        }

        // Add ';' to make parsing easier.
        csi = csi.substring(0, csi.length() - 1) + ";";

        Matcher rgbMatcher = rgbCSI.matcher(csi);

        int len = csi.length();
        int start = 0;
        int end;
        while ((end = csi.indexOf(';', start)) > start)
        {
            String code = csi.substring(start, end);
            start = end + 1;

            if (code.isEmpty() || code.equals("0"))
            {
                reset();
                continue;
            }
            int c;
            try
            {
                c = Integer.parseInt(code);
            }
            catch (NumberFormatException e)
            {
                logger.log(Level.FINE,
                    "Ignoring invalid CSI command \"" + code + "\"", e);
                continue;
            }

            switch (c)
            {
                case 1:
                    bold = true;
                    break;
                case 3:
                    italic = true;
                    break;
                case 4:
                    underline = true;
                    break;
                case 5:
                case 6:
                    blink = true;
                    break;
                case 7:
                    inverse = true;
                    break;
                case 8:
                    hidden = true;
                    break;
                case 9:
                    strikethrough = true;
                    break;
                case 20:
                    fraktur = true;
                    break;
                case 21:
                    underline = true;   // Double underline, really
                    bold = false;
                    break;
                case 22:
                    bold = false;
                    break;
                case 23:
                    italic = false;
                    fraktur = false;
                    break;
                case 24:
                    underline = false;
                    break;
                case 25:
                    blink = false;
                    break;
                case 27:
                    inverse = false;
                    break;
                case 28:
                    hidden = false;
                    break;
                case 29:
                    strikethrough = false;
                    break;
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 90:
                case 91:
                case 92:
                case 93:
                case 94:
                case 95:
                case 96:
                case 97:
                    color = colors.get(c, bold);
                    break;
                case 38:
                    if (rgbMatcher.region(start, len).lookingAt())
                    {
                        color = parseRGB(rgbMatcher);

                        start = rgbMatcher.end();
                        if (start < len)
                        {
                            start++;
                        }
                    }
                    break;
                case 39:
                    color = null;
                    break;
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 100:
                case 101:
                case 102:
                case 103:
                case 104:
                case 105:
                case 106:
                case 107:
                    background = colors.get(c - 10, bold);
                    break;
                case 48:
                    if (rgbMatcher.region(start, len).lookingAt())
                    {
                        background = parseRGB(rgbMatcher);

                        start = rgbMatcher.end();
                        if (start < len)
                        {
                            start++;
                        }
                    }
                    break;
                case 49:
                    background = null;
                    break;
                case 51:
                    enclosure = Enclosure.FRAMED;
                    break;
                case 52:
                    enclosure = Enclosure.CIRCLED;
                    break;
                case 53:
                    overline = true;
                    break;
                case 54:
                    enclosure = null;
                    break;
                case 55:
                    overline = false;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Translates 8-bit color table value to a Color instance.
     *
     * @see <a href="https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit">ANSI
     *      escape codes &rarr; Colors &rarr; 8-bit on Wikipedia</a>
     */
    private Color lookupCSIColorTable(int index)
    {
        index &= 255;

        Color value;
        if (index < 16)
        {
            value = colors.get(index % 8, index >= 8);
        }
        else if (index >= 232)
        {
            value = Color.gray((index - 232) / 23.0);
        }
        else
        {
            index -= 16;
            int red = index / 36;
            index -= red * 36;
            int green = index / 6;
            index -= green * 6;
            int blue = index;
            value = Color.color(red / 5.0, green / 5.0, blue / 5.0);
        }

        return value;
    }

    private Color parseRGB(Matcher rgbMatcher)
    {
        if (rgbMatcher.group().startsWith("2"))
        {
            int red =   Integer.parseInt(rgbMatcher.group(1)) & 255;
            int green = Integer.parseInt(rgbMatcher.group(2)) & 255;
            int blue =  Integer.parseInt(rgbMatcher.group(3)) & 255;
            return Color.rgb(red, green, blue);
        }
        else
        {
            int index = Integer.parseInt(rgbMatcher.group(4));
            return lookupCSIColorTable(index);
        }
    }
}
