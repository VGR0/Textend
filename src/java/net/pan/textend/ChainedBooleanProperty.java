package net.pan.textend;

import java.util.Objects;
import java.util.function.Function;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.value.ObservableValue;

/**
 * A boolean sub-property of another property.  This notifies listeners when
 * the owning property changes as well as when the value itself changes.
 *
 * @param <B> type of property containing bean which owns this property
 */
class ChainedBooleanProperty<B>
extends BooleanPropertyBase
{
    private static final Logger logger =
        Logger.getLogger(ChainedBooleanProperty.class.getName());

    private final ObservableValue<B> beanProperty;

    private final Function<B, BooleanProperty> valueProperty;

    ChainedBooleanProperty(ObservableValue<B> beanProperty,
                           Function<B, BooleanProperty> valueProperty)
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
        final boolean old;
        if (logger.isLoggable(Level.FINEST) && oldValue != null)
        {
            old = valueProperty.apply(oldValue).get();
        }
        else
        {
            old = false;
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

    private void propertyChanged(ObservableValue<? extends Boolean> source,
                                 Boolean oldValue,
                                 Boolean newValue)
    {
        fireValueChangedEvent();
    }

    @Override
    public boolean get()
    {
        B bean = beanProperty.getValue();
        return (bean != null && valueProperty.apply(bean).get());
    }

    @Override
    public void set(boolean value)
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
