package net.pan.textend; 

import javafx.util.converter.IntegerStringConverter;

class PortConverter
extends IntegerStringConverter
{
    @Override
    public Integer fromString(String s)
    {
        Integer value = super.fromString(s);

        if (value == null || value < 1 || value > 65535)
        {
            // Do we need i18n here?
            throw new IllegalArgumentException("Invalid port: " + value);
        }

        return value;
    }
}
