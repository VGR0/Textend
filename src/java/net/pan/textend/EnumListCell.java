package net.pan.textend;

import java.util.Map;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Collections;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ListCell;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;

class EnumListCell<E extends Enum<E>>
extends ListCell<E>
{
    private final Map<E, String> valueLabels;

    EnumListCell(Class<E> enumClass,
                 ResourceBundle res)
    {
        this(enumClass, res, null);
    }

    EnumListCell(Class<E> enumClass,
                 ResourceBundle res,
                 String keyPrefix)
    {
        valueLabels = getNames(enumClass, res,
            Objects.toString(keyPrefix, enumClass.getSimpleName() + "."));
    }

/*
    EnumListCell(Class<E> enumClass,
                 ResourceBundle res,
                 String keyPrefix,
                 String nullKey)
    {
        Map<E, String> labels = getNames(enumClass, res,
            Objects.toString(keyPrefix, enumClass.getSimpleName() + "."));
        labels = new HashMap<E, String>(labels);
        labels.put(null, res.getString(nullKey));

        valueLabels = Collections.unmodifiableMap(labels);
    }
*/

    private EnumListCell(Map<E, String> labels)
    {
        this.valueLabels = labels;
    }

    static <E extends Enum<E>> Map<E, String> getNames(Class<E> enumClass,
                                                       ResourceBundle res)
    {
        return getNames(enumClass, res, enumClass.getSimpleName() + ".");
    }

    static <E extends Enum<E>> Map<E, String> getNames(Class<E> enumClass,
                                                       ResourceBundle res,
                                                       String keyPrefix)
    {
        Objects.requireNonNull(enumClass, "Enum class cannot be null");
        Objects.requireNonNull(res, "ResourceBundle cannot be null");
        Objects.requireNonNull(keyPrefix, "Key prefix cannot be null");

        Map<E, String> map = new EnumMap<>(enumClass);
        for (E value : enumClass.getEnumConstants())
        {
            map.put(value, res.getString(keyPrefix + value));
        }

        return Collections.unmodifiableMap(map);
    }

    @Override
    protected void updateItem(E item,
                              boolean empty)
    {
        super.updateItem(item, empty);

        setText(item == null || empty ? null : valueLabels.get(item));
    }

/*
    static <E extends Enum<E>> ComboBox<E> createEnumList(Class<E> enumClass,
                                                          ResourceBundle res,
                                                          String nullKey)
    {
        ComboBox<E> list = new ComboBox<>();
        list.getItems().add(null);
        list.getItems().addAll(enumClass.getEnumConstants());
        list.setButtonCell(new EnumListCell<>(enumClass, res, null, nullKey));
        list.setCellFactory(
            l -> new EnumListCell<>(enumClass, res, null, nullKey));
        list.setValue(list.getItems().get(0));
        list.setMinWidth(Region.USE_PREF_SIZE);
        return list;
    }
*/

    static <E extends Enum<E>> ComboBox<E> createEnumList(Class<E> enumClass,
                                                          ResourceBundle res)
    {
        ComboBox<E> list = new ComboBox<>();
        list.getItems().addAll(enumClass.getEnumConstants());
        list.setButtonCell(new EnumListCell<>(enumClass, res));
        list.setCellFactory(l -> new EnumListCell<>(enumClass, res));
        list.setValue(list.getItems().get(0));
        list.setMinWidth(Region.USE_PREF_SIZE);
        return list;
    }

    static <E extends Enum<E>> ComboBox<E> createEnumList(Class<E> enumClass,
                                                          ResourceBundle res,
                                                          String keyPrefix)
    {
        ComboBox<E> list = new ComboBox<>();
        list.getItems().addAll(enumClass.getEnumConstants());
        list.setButtonCell(new EnumListCell<>(enumClass, res, keyPrefix));
        list.setCellFactory(l -> new EnumListCell<>(enumClass, res, keyPrefix));
        list.setValue(list.getItems().get(0));
        list.setMinWidth(Region.USE_PREF_SIZE);
        return list;
    }
}
