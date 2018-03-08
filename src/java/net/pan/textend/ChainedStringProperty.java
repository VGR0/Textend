package net.pan.textend;

import java.util.Objects;
import java.util.function.Function;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.ObservableValue;

/**
 * A String sub-property of another property.  This notifies listeners when
 * the owning property changes as well as when the value itself changes.
 *
 * @param <B> type of property containing bean which owns this property
 */
class ChainedStringProperty<B>
extends StringPropertyBase
{
    private static final Logger logger =
        Logger.getLogger(ChainedStringProperty.class.getName());

    private final ObservableValue<B> beanProperty;

    private final Function<B, StringProperty> valueProperty;

    ChainedStringProperty(ObservableValue<B> beanProperty,
                          Function<B, StringProperty> valueProperty)
    {
        this.beanProperty = Objects.requireNonNull(beanProperty,
            "Bean property cannot be null");
        this.valueProperty = Objects.requireNonNull(valueProperty,
            "Value property function cannot be null");

        beanProperty.addListener(this::beanChanged);
    }

    private void beanChanged(ObservableValue<? extends B> source,
                             B oldValue,
                             B newValue)
    {
        final Object old;
        if (logger.isLoggable(Level.FINEST) && oldValue != null)
        {
            old = valueProperty.apply(oldValue).get();
        }
        else
        {
            old = null;
        }

        if (oldValue != null)
        {
            valueProperty.apply(oldValue).removeListener(this::propertyChanged);
        }
        if (newValue != null)
        {
            valueProperty.apply(newValue).addListener(this::propertyChanged);
        }

        logger.finest(() ->
            String.format("beanChanged: %s (%s) -> %s (%s), value: %s -> %s%n",
            oldValue, System.identityHashCode(oldValue),
            newValue, System.identityHashCode(newValue),
            old, get()));

        fireValueChangedEvent();
    }

    private void propertyChanged(ObservableValue<? extends String> source,
                                 String oldValue,
                                 String newValue)
    {
        fireValueChangedEvent();
    }

    @Override
    public String get()
    {
        B bean = beanProperty.getValue();
        return (bean == null ? null : valueProperty.apply(bean).get());
    }

    @Override
    public void set(String value)
    {
        B bean = beanProperty.getValue();
        if (bean != null)
        {
            valueProperty.apply(bean).set(value);
        }
    }

    @Override
    public Object getBean()
    {
        return beanProperty.getValue();
    }

    @Override
    public String getName()
    {
        B bean = beanProperty.getValue();
        return (bean == null ? null : valueProperty.apply(bean).getName());
    }
}
