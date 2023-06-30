package info.kgeorgiy.ja.samodelov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final InvertibleList<T> list;
    private final Comparator<T> comparator;

    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(final Collection<T> collection) {
        this(collection, null);
    }

    public ArraySet(final Collection<T> collection, final Comparator<T> comparator) {
        final Set<T> set = new TreeSet<>(comparator);
        set.addAll(collection);
        this.list = new InvertibleList<>(List.copyOf(set), false);
        this.comparator = comparator;
    }

    private ArraySet(final List<T> list, final Comparator<T> comparator, final boolean isInverted) {
        this.list = new InvertibleList<>(list, isInverted);
        this.comparator = comparator;
    }

    @Override
    public T lower(final T t) {
        return getElement(t, false, false);
    }

    @Override
    public T floor(final T t) {
        return getElement(t, false, true);
    }

    @Override
    public T ceiling(final T t) {
        return getElement(t, true, true);
    }

    @Override
    public T higher(final T t) {
        return getElement(t, true, false);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(list.items, Collections.reverseOrder(comparator), !list.isInverted);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }


    @SuppressWarnings("unchecked")
    private Comparator<T> getCorrectComparator() {
        return (comparator == null) ? (Comparator<T>) Comparator.naturalOrder() : comparator;
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        if (getCorrectComparator().compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Incorrect format of input data: fromElement > toElement");
        }
        return getSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    // :NOTE: minor copy-paste
    @Override
    public NavigableSet<T> headSet(final T toElement, final boolean inclusive) {
        if (isEmpty()) {
            return this;
        } else {
            return getSubSet(first(), true, toElement, inclusive);
        }
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, final boolean inclusive) {
        if (isEmpty()) {
            return this;
        } else {
            return getSubSet(fromElement, inclusive, last(), true);
        }
    }

    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    private boolean isValidIndex(final int index) {
        return (0 <= index && index < size());
    }

    private int getIndex(final T element, final boolean isFrom, final boolean isInclusive) {
        final int ifNotExist = isFrom ? 0 : -1;
        final int ifExist = isInclusive ? 0 : 1 + 2 * ifNotExist;

        final int index = Collections.binarySearch(list, element, comparator);
        final int uncheckedAnswer = (index < 0) ? -(index + 1) + ifNotExist : index + ifExist;
        return isValidIndex(uncheckedAnswer) ? uncheckedAnswer : -1;
    }

    private T getElement(final T t, final boolean isFrom, final boolean isInclusive) {
        final int index = getIndex(t, isFrom, isInclusive);
        return isValidIndex(index) ? list.get(index) : null;
    }


    private NavigableSet<T> getSubSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        final int from = getIndex(fromElement, true, fromInclusive);
        final int to = getIndex(toElement, false, toInclusive);

        if (!isValidIndex(to) || !isValidIndex(from) || from > to) {
            // :NOTE: emptyList
            return new ArraySet<>(Collections.emptyList(), comparator, list.isInverted);
        }
        return new ArraySet<>(list.subList(from, to + 1), comparator, list.isInverted);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public int size() {
        return list.size();
    }

    // :NOTE: isEmpty?
    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
//        new TreeSet<>().contains()
        try {
            return Collections.binarySearch(list, (T) o, comparator) >= 0;
        } catch (final ClassCastException e) {
            return false;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    private T getElementFromIndex(int index, String nameOfIndex) {
        if (!isEmpty()) {
            return list.get(index);
        } else {
            throw new NoSuchElementException("Try to get " + nameOfIndex + " element from empty set");
        }
    }
    @Override
    public T first() {
       return getElementFromIndex(0, "first");
    }

    @Override
    public T last() {
        return getElementFromIndex(size() - 1, "second");
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    private static class InvertibleList<E> extends AbstractList<E> implements RandomAccess {
        private final List<E> items;
        private final boolean isInverted;

        private InvertibleList(final List<E> items, final boolean isInverted) {
            this.items = items;
            this.isInverted = isInverted;
        }

        @Override
        public E get(final int index) {
            return items.get(isInverted ? items.size() - 1 - index : index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }
}
