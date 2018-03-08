package net.pan.textend;

import java.util.Set;
import java.util.EnumSet;

import java.util.Map;
import java.util.EnumMap;
import java.util.HashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javafx.scene.paint.Color;

/**
 * Maps ANSI CSI color codes to Color values.  Created from
 * a {@link ANSIPalette.Builder} or from 
 * {@link #get(ANSIPalette.StandardPalette)}.
 */
class ANSIPalette
{
    private static final Collection<Integer> requiredKeys =
        Set.of(30, 31, 32, 33, 34, 35, 36, 37);

    private final Map<Integer, Map<Boolean, Color>> colors;

    private ANSIPalette(Map<Integer, Map<Boolean, Color>> colors)
    {
        this.colors = Collections.unmodifiableMap(colors);
    }

    /**
     * Creates new {@link ANSIPalette} instances.
     */
    static class Builder
    {
        private final Map<Integer, Map<Boolean, Color>> colors =
            new HashMap<>(16);

        /**
         * Specifies normal and bright colors for a CSI code.
         *
         * @param code CSI code, 30-37
         * @param dark "normal" color for specified CSI code
         * @param bright "bright" color for specified CSI code
         */
        void add(int code,
                 Color dark,
                 Color bright)
        {
            Map<Boolean, Color> shades = new HashMap<>(2);
            shades.put(false, dark);
            shades.put(true, bright);
            colors.put(code, Collections.unmodifiableMap(shades));
        }

        /**
         * Creates a new ANSIPalette from this instance's colors.
         * 
         * @throws IllegalStateException if colors have not been added
         *                               for every value from 30 to 37,
         *                               inclusive
         */
        ANSIPalette build()
        {
            if (!colors.keySet().containsAll(requiredKeys))
            {
                throw new IllegalStateException(
                    "All of the following CSI codes must be added" +
                    " before building: " + requiredKeys);
            }
            return new ANSIPalette(new HashMap<>(colors));
        }
    }

    Color get(int code,
              boolean bright)
    {
        if (code >= 90 && code <= 97)
        {
            bright = true;
            code -= 60;
        }

        if (code < 30 || code > 37)
        {
            return null;
        }
        return colors.get(code).get(bright);
    }

    /**
     * Various interpretations of standard ANSI color values.
     *
     * @see <a href="https://en.wikipedia.org/wiki/ANSI_escape_code#3/4_bit">ANSI
     *      escape codes &rarr; Colors &rarr; 3/4 bit on Wikipedia</a>
     */
    enum StandardPalette
    {
        /** Palette based on standard JavaFX color values. */
        DEFAULT,

        VGA,
        WINDOWS,
        MAC,
        PUTTY,
        MIRC,
        XTERM,
        X11,
        UBUNTU
    }

    private static final Map<StandardPalette, ANSIPalette> standardPalettes =
        new EnumMap<>(StandardPalette.class);

    static ANSIPalette get(StandardPalette type)
    {
        Objects.requireNonNull(type, "Palette type cannot be null");
        if (standardPalettes.isEmpty())
        {
            standardPalettes.put(StandardPalette.DEFAULT, createDefault());
            standardPalettes.put(StandardPalette.VGA,     createVGA());
            standardPalettes.put(StandardPalette.WINDOWS, createWindows());
            standardPalettes.put(StandardPalette.MAC,     createMac());
            standardPalettes.put(StandardPalette.PUTTY,   createPuTTY());
            standardPalettes.put(StandardPalette.MIRC,    createMIRC());
            standardPalettes.put(StandardPalette.XTERM,   createXterm());
            standardPalettes.put(StandardPalette.X11,     createX11());
            standardPalettes.put(StandardPalette.UBUNTU,  createUbuntu());

            assert standardPalettes.keySet().containsAll(
                EnumSet.allOf(StandardPalette.class)) :
                "Not all StandardPalette values accounted for";
        }
        return standardPalettes.get(type);
    }

    private static ANSIPalette createDefault()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,         Color.DARKGRAY);
        colors.add(31, Color.DARKRED,       Color.RED);
        colors.add(32, Color.DARKGREEN,     Color.GREEN);
        colors.add(33, Color.BROWN,         Color.YELLOW);
        colors.add(34, Color.DARKBLUE,      Color.BLUE);
        colors.add(35, Color.DARKMAGENTA,   Color.MAGENTA);
        colors.add(36, Color.DARKCYAN,      Color.CYAN);
        colors.add(37, Color.GRAY,          Color.WHITE);
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createVGA()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,               Color.rgb(85, 85, 85));
        colors.add(31, Color.rgb(170, 0, 0),      Color.rgb(255, 85, 85));
        colors.add(32, Color.rgb(0, 170, 0),      Color.rgb(85, 255, 85));
        colors.add(33, Color.rgb(170, 85, 0),     Color.rgb(255, 255, 85));
        colors.add(34, Color.rgb(0, 0, 170),      Color.rgb(85, 85, 255));
        colors.add(35, Color.rgb(170, 0, 170),    Color.rgb(255, 85, 255));
        colors.add(36, Color.rgb(0, 170, 170),    Color.rgb(85, 255, 255));
        colors.add(37, Color.rgb(170, 170, 170),  Color.rgb(255, 255, 255));
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createWindows()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,               Color.rgb(128, 128, 128));
        colors.add(31, Color.rgb(128, 0, 0),      Color.rgb(255, 0, 0));
        colors.add(32, Color.rgb(0, 128, 0),      Color.rgb(0, 255, 0));
        colors.add(33, Color.rgb(128, 128, 0),    Color.rgb(255, 255, 0));
        colors.add(34, Color.rgb(0, 0, 128),      Color.rgb(0, 0, 255));
        colors.add(35, Color.rgb(128, 0, 128),    Color.rgb(255, 0, 255));
        colors.add(36, Color.rgb(0, 128, 128),    Color.rgb(0, 255, 255));
        colors.add(37, Color.rgb(192, 192, 192),  Color.rgb(255, 255, 255));
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createMac()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,               Color.rgb(129, 131, 131));
        colors.add(31, Color.rgb(194, 54, 33),    Color.rgb(252, 57, 31));
        colors.add(32, Color.rgb(37, 188, 36),    Color.rgb(49, 231, 34));
        colors.add(33, Color.rgb(173, 173, 39),   Color.rgb(234, 236, 35));
        colors.add(34, Color.rgb(73, 46, 225),    Color.rgb(88, 51, 255));
        colors.add(35, Color.rgb(211, 56, 211),   Color.rgb(249, 53, 248));
        colors.add(36, Color.rgb(51, 187, 200),   Color.rgb(20, 240, 240));
        colors.add(37, Color.rgb(203, 204, 205),  Color.rgb(233, 235, 235));
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createPuTTY()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,               Color.rgb(85, 85, 85));
        colors.add(31, Color.rgb(187, 0, 0),      Color.rgb(255, 85, 85));
        colors.add(32, Color.rgb(0, 187, 0),      Color.rgb(85, 255, 85));
        colors.add(33, Color.rgb(187, 187, 0),    Color.rgb(255, 255, 86));
        colors.add(34, Color.rgb(0, 0, 187),      Color.rgb(85, 85, 255));
        colors.add(35, Color.rgb(187, 0, 187),    Color.rgb(255, 85, 255));
        colors.add(36, Color.rgb(0, 187, 187),    Color.rgb(85, 255, 255));
        colors.add(37, Color.rgb(187, 187, 187),  Color.rgb(255, 255, 255));
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createMIRC()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,               Color.rgb(127, 127, 127));
        colors.add(31, Color.rgb(127, 0, 0),      Color.rgb(255, 0, 0));
        colors.add(32, Color.rgb(0, 147, 0),      Color.rgb(0, 252, 0));
        colors.add(33, Color.rgb(252, 127, 0),    Color.rgb(255, 255, 0));
        colors.add(34, Color.rgb(0, 0, 127),      Color.rgb(0, 0, 252));
        colors.add(35, Color.rgb(156, 0, 156),    Color.rgb(255, 0, 255));
        colors.add(36, Color.rgb(0, 147, 147),    Color.rgb(0, 255, 255));
        colors.add(37, Color.rgb(210, 210, 210),  Color.rgb(255, 255, 255));
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createXterm()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,               Color.rgb(127, 127, 127));
        colors.add(31, Color.rgb(205, 0, 0),      Color.rgb(255, 0, 0));
        colors.add(32, Color.rgb(0, 205, 0),      Color.rgb(0, 255, 0));
        colors.add(33, Color.rgb(205, 205, 0),    Color.rgb(255, 255, 0));
        colors.add(34, Color.rgb(0, 0, 238),      Color.rgb(92, 92, 255));
        colors.add(35, Color.rgb(205, 0, 205),    Color.rgb(255, 0, 255));
        colors.add(36, Color.rgb(0, 205, 205),    Color.rgb(0, 255, 255));
        colors.add(37, Color.rgb(229, 229, 229),  Color.rgb(255, 255, 255));
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createX11()
    {
        Builder colors = new Builder();
        colors.add(30, Color.BLACK,               Color.rgb(190, 190, 190));
        colors.add(31, Color.rgb(255, 0, 0),      Color.rgb(240, 128, 128));
        colors.add(32, Color.rgb(0, 255, 0),      Color.rgb(144, 238, 144));
        colors.add(33, Color.rgb(255, 255, 0),    Color.rgb(255, 255, 224));
        colors.add(34, Color.rgb(0, 0, 255),      Color.rgb(173, 216, 230));
        colors.add(35, Color.rgb(255, 0, 255),    Color.rgb(221, 160, 221));
        colors.add(36, Color.rgb(0, 255, 255),    Color.rgb(224, 255, 255));
        colors.add(37, Color.rgb(255, 255, 255),  Color.rgb(255, 255, 255));
        return colors.build();
    }

    /** See {@link #ANSIPalette.StandardPalette} for source. */
    private static ANSIPalette createUbuntu()
    {
        Builder colors = new Builder();
        colors.add(30, Color.rgb(1, 1, 1),        Color.rgb(128, 128, 128));
        colors.add(31, Color.rgb(222, 56, 43),    Color.rgb(255, 0, 0));
        colors.add(32, Color.rgb(57, 181, 74),    Color.rgb(0, 255, 0));
        colors.add(33, Color.rgb(255, 199, 6),    Color.rgb(255, 255, 0));
        colors.add(34, Color.rgb(0, 111, 184),    Color.rgb(0, 0, 255));
        colors.add(35, Color.rgb(118, 38, 113),   Color.rgb(255, 0, 255));
        colors.add(36, Color.rgb(44, 181, 233),   Color.rgb(0, 255, 255));
        colors.add(37, Color.rgb(204, 204, 204),  Color.rgb(255, 255, 255));
        return colors.build();
    }
}
