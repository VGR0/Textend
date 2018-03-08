package net.pan.textend;

import java.util.Objects;
import java.util.function.Function;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ObservableValue;

/**
 * A sub-property of another property.  This notifies listeners when
 * the owning property changes as well as when the value itself changes.
 *
 * @param <T> this property's value type
 * @param <B> type of property containing bean which owns this property
 */
class ChainedObjectProperty<T, B>
extends ObjectPropertyBase<T>
{
    private static final Logger logger =
        Logger.getLogger(ChainedObjectProperty.class.getName());

    private final ObservableValue<B> beanProperty;

    private final Function<B, ObjectProperty<T>> valueProperty;

    ChainedObjectProperty(ObservableValue<B> beanProperty,
                          Function<B, ObjectProperty<T>> valueProperty)
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

    private void propertyChanged(ObservableValue<? extends T> source,
                                 T oldValue,
                                 T newValue)
    {
        fireValueChangedEvent();
    }

    @Override
    public T get()
    {
        B bean = beanProperty.getValue();
        return (bean == null ? null : valueProperty.apply(bean).get());
    }

    @Override
    public void set(T value)
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
