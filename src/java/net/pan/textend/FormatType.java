package net.pan.textend;

import java.time.format.FormatStyle;

/**
 * Wrapper for nullable {@code FormatStyle} value.
 */
public enum FormatType
{
    NONE(null),
    SHORT(FormatStyle.SHORT),
    MEDIUM(FormatStyle.MEDIUM),
    LONG(FormatStyle.LONG),
    FULL(FormatStyle.FULL);

    /** @serial */
    final FormatStyle style;

    private FormatType(FormatStyle style)
    {
        this.style = style;
    }
}
