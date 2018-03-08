package net.pan.textend;

public enum PatternType
{
    TEXT_CASELESS(false),
    TEXT(true),
    GLOB_CASELESS(false),
    GLOB(true),
    REGEX(true);

    /** @serial */
    private final boolean caseSensitive;

    private PatternType(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    boolean isCaseSensitive()
    {
        return caseSensitive;
    }
}
