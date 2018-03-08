package net.pan.textend;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Maps {@code int} constants to their field names, for use with debugging.
 */
class TelnetByteNames
{
    private final Map<Integer, String> names;

    TelnetByteNames(Class<?> constantsClass)
    {
        try
        {
            Field[] fields = constantsClass.getDeclaredFields();
            Map<Integer, String> valueToName = new HashMap<>(fields.length);
            for (Field field : fields)
            {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) && Modifier.isFinal(mods) &&
                    field.getType().equals(int.class))
                {
                    int value = field.getInt(null);
                    valueToName.put(value, field.getName());
                }
            }

            names = Collections.unmodifiableMap(valueToName);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    String getName(int constantValue)
    {
        return names.get(constantValue);
    }

    String getName(int constantValue,
                   String defaultName)
    {
        return names.getOrDefault(constantValue, defaultName);
    }
}
