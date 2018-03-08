package net.pan.textend;

import java.util.LinkedList;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Ordered list of previously entered values.  Maintains a position within
 * the entire list of items;  committing an item adds it to the end of the list,
 * and moves the internal position to the end of the list.
 * <p>
 * There can also be a single "uncommitted" item which is always at the end
 * of the list, but is not part of the actual history, unless later committed.
 * The {@code setCurrent} method sets this value.  Committing a different
 * value will clear the uncommitted value.  This is generally the "currently
 * edited" value.
 *
 * @param <T> type of data kept in History instance
 */
class History<T>
{
    private static final Logger logger =
        Logger.getLogger(History.class.getName());

    private final LinkedList<T> list = new LinkedList<>();

    /**
     * Maximum number of items this instance can hold.  An attempt to add
     * more than this will result in the oldest item being discarded.
     */
    private final int size;

    /** Current history index, or -1 when preparing new item. */
    private int index = -1;

    private T uncommitted;

    History(int size)
    {
        if (size < 1)
        {
            throw new IllegalArgumentException("Size must be positive");
        }

        this.size = size;
    }

    /**
     * Sets this instance's final item and commits it.
     *
     * @param item data to commit to this instance's list
     */
    void commit(T item)
    {
        logger.finer(() -> "Committing " + item);

        add(item);
        index = -1;
        uncommitted = null;
    }

    private void add(T item)
    {
        if (!item.equals(list.peekLast()))  // avoid duplicates
        {
            logger.fine(() -> "Adding " + item);
            list.add(item);
            if (list.size() > size)
            {
                list.removeFirst();
            }

            index = -1;
        }
    }

    /**
     * Sets this instance's final, uncommitted item, if any only if the
     * internal history index is not pointing to a location in the history
     * list.  In other words, calling this while traversing the history items
     * will have no effect.
     *
     * @param item data to use as the new uncommitted item
     */
    void setCurrent(T item)
    {
        if (index < 0)
        {
            uncommitted = item;
        }
    }

    /**
     * Decrements this instance's internal position.
     *
     * @return item at new position, or {@code null} if already at beginning
     */
    T previous()
    {
        logger.fine(() -> "index=" + index + ", size=" + list.size());
        logger.finer(() -> "list=" + list);

        if (index < 0)
        {
            index = list.size();
        }
        if (index > 0 && index <= list.size())
        {
            logger.fine(() -> "Returning " + list.get(index - 1));
            return list.get(--index);
        }
        return null;
    }

    /**
     * Increments this instance's internal position.
     *
     * @return item at new position, or {@code null} if already at end
     */
    T next()
    {
        logger.fine(() -> "index=" + index + ", size=" + list.size());
        logger.finer(() -> "list=" + list);

        if (index >= 0 && index < list.size() - 1)
        {
            logger.fine(() -> "Returning " + list.get(index + 1));
            return list.get(++index);
        }
        index = -1;
        return uncommitted;
    }
}
