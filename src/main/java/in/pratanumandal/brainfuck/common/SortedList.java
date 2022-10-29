package in.pratanumandal.brainfuck.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class SortedList<E> extends LinkedList<E>
{
    private Comparator<E> comparator;

    public SortedList(final Comparator<E> comparator) {
        this.comparator = comparator;
    }

    @Override
    public void add(int index, E element) {
        // ignore index
        this.add(element);
    }

    @Override
    public boolean add(final E element) {
        final boolean result = super.add(element);
        Collections.sort(this, this.comparator);
        return result;
    }

}
