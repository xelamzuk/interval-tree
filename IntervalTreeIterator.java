package com.kg.smartfactory.common.interval.tree;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.*;

public class IntervalTreeIterator<T> implements Iterator<Interval<T>> {

    private IteratorDirection direction;

    private long leftBorder;
    private long rightBorder;

    private IntervalNode<T> currentNode;
    private Interval<T> currentInterval;
    private IntervalNode<T> currentParent;

    private Iterator<Map.Entry<Interval<T>, Long>> intervalsMapIterator;
    private long currentIntervalCount;

    private Interval<T> foundNextInterval;

    private Set<IntervalNode> visitedNodes = Sets.newHashSet();


    SortedMap<Interval<T>, Long> reversedIntervals = new TreeMap<Interval<T>, Long>(new Comparator<Interval<T>>() {
        @Override
        public int compare(Interval<T> interval1, Interval<T> interval2) {
            if (interval1.getEnd() < interval2.getEnd()) {
                return 1;
            } else if (interval1.getEnd() > interval2.getEnd()) {
                return -1;
            } else if (interval1.getStart() < interval2.getStart()) {
                return 1;
            } else if (interval1.getStart() > interval2.getStart()) {
                return -1;
            } else if ((interval1.getData() != null) && (interval2.getData() != null)) {
                return interval1.getData().toString().compareTo(interval2.getData().toString());
            }
            return 0;
        }
    });

    public IntervalTreeIterator(long border, IntervalNode<T> head, IteratorDirection direction) {
        this.direction = direction;
        switch (direction) {
            case BACKWARD: {
                getLastIntervalInBorders(border, head);
                break;
            }
            case FORWARD: {
                getFirstIntervalInBorders(border, head);
                break;
            }
            default: {
                throw new IllegalStateException("Unknown direction:" + direction);
            }
        }
    }

    private void getLastIntervalInBorders(long rightBorder, IntervalNode<T> head) {
        this.rightBorder = rightBorder; // эквивалент интервала включающего в себя границу
        findRightMostInterval(head);
    }

    private void getFirstIntervalInBorders(long leftBorder, IntervalNode<T> head) {
        this.leftBorder = leftBorder; // эквивалент интервала включающего в себя границу
        findLeftMostInterval(head);
    }

    public boolean hasNext() {
        if (foundNextInterval == null) {
            foundNextInterval = next();
        }

        return foundNextInterval != null;
    }

    public Interval<T> next() {
        if (foundNextInterval != null) {
            Interval<T> result = foundNextInterval;
            foundNextInterval = null;
            return result;
        }

        if (IteratorDirection.BACKWARD.equals(direction)) {
            return getPrevious();
        } else if (IteratorDirection.FORWARD.equals(direction)) {
            Interval<T> result = getNext();
            return result;
        }

        throw new IllegalStateException("Unknown direction: " + direction);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private Interval<T> getNext() {
        while (true) {

            if (currentIntervalCount > 0) {
                currentIntervalCount--;
                return currentInterval;
            }

            if (intervalsMapIterator != null) {
                if (intervalsMapIterator.hasNext()) {
                    Map.Entry<Interval<T>, Long> entry = intervalsMapIterator.next();
                    Interval<T> interval = entry.getKey();
                    if (interval.getStart() >= leftBorder) {
                        currentInterval = interval;
                        currentIntervalCount = entry.getValue();
                    }
                } else {
                    intervalsMapIterator = null;
                }
                continue;
            } else if (currentNode.getRight() != null && currentNode.getRight().getCenter() < leftBorder) {
                visitedNodes.add(currentNode);
                currentNode = currentNode.getRight();
                continue;
            } else if (currentNode.getRight() != null && currentNode.getRight().getCenter() >= leftBorder) {
                findLeftMostInterval(currentNode.getRight());
                visitedNodes.add(currentNode);
                continue;
            } else {
                while ((currentParent != null) && (visitedNodes.contains(currentParent) || (currentParent.getCenter() < leftBorder))) {
                    currentParent = currentParent.getParent();
                }

                if (currentParent != null) {
                    currentNode = currentParent;
                    currentParent = currentParent.getParent();
                    Set<Map.Entry<Interval<T>, Long>> entries = currentNode.getIntervals().entrySet();
                    intervalsMapIterator = entries.iterator();
                    visitedNodes.add(currentNode);
                    continue;
                }
            }

            return null;
        }
    }

    private Interval<T> getPrevious() {
        while (true) {
            if (currentIntervalCount > 0) {
                currentIntervalCount--;
                return currentInterval;
            }
            if (intervalsMapIterator != null) {
                if (intervalsMapIterator.hasNext()) {
                    Map.Entry<Interval<T>, Long> entry = intervalsMapIterator.next();
                    Interval<T> interval = entry.getKey();
                    if (interval.getEnd() <= rightBorder) {
                        currentInterval = interval;
                        currentIntervalCount = entry.getValue();
                    }
                } else {
                    intervalsMapIterator = null;
                }
                continue;
            } else if (currentNode.getLeft() != null && currentNode.getLeft().getCenter() > rightBorder) {
                visitedNodes.add(currentNode);
                currentNode = currentNode.getLeft();
                continue;
            } else if (currentNode.getLeft() != null && currentNode.getLeft().getCenter() <= rightBorder) {
                visitedNodes.add(currentNode);
                findRightMostInterval(currentNode.getLeft());
                continue;
            } else {
                while ((currentParent != null) && (visitedNodes.contains(currentParent) || (currentParent.getCenter() > rightBorder))) {
                    currentParent = currentParent.getParent();
                }
                if ((currentParent != null)) {
                    currentNode = currentParent;
                    currentParent = currentParent.getParent();
                    reversedIntervals.clear();
                    reversedIntervals.putAll(currentNode.getIntervals());
                    Set<Map.Entry<Interval<T>, Long>> entries = reversedIntervals.entrySet();
                    intervalsMapIterator = entries.iterator();
                    visitedNodes.add(currentNode);
                    continue;
                }
            }

            return null;
        }
    }

    private void findRightMostInterval(IntervalNode<T> node) {
        Queue<IntervalNode<T>> nodes = Lists.newLinkedList();
        nodes.add(node);

        while (!nodes.isEmpty()) {
            currentNode = nodes.poll();

            setCurrentParent(currentNode.getParent());

            if (currentNode.getCenter() <= rightBorder) {
                if (currentNode.getRight() != null) {
                    nodes.add(currentNode.getRight());
                    continue;
                }

                getLastIntervalOnCurrentNode();
            }
        }
    }

    private void findLeftMostInterval(IntervalNode<T> node) {
        Queue<IntervalNode<T>> nodes = Lists.newLinkedList();
        nodes.add(node);

        while (nodes.size() > 0) {
            currentNode = nodes.poll();

            setCurrentParent(currentNode.getParent());

            if (currentNode.getCenter() >= leftBorder) {
                if (currentNode.getLeft() != null) {
                    nodes.add(currentNode.getLeft());
                    continue;
                }
                getFirstIntervalOnCurrentNode();
            }
        }
    }

    private void setCurrentParent(IntervalNode<T> currentParent) {
        this.currentParent = currentParent;
    }

    private void setCurrentIntervalCount(long currentIntervalCount) {
        this.currentIntervalCount = currentIntervalCount;
    }

    private void setIntervalsMapIterator(Iterator<Map.Entry<Interval<T>, Long>> intervalListIterator) {
        this.intervalsMapIterator = intervalListIterator;
    }

    private void getFirstIntervalOnCurrentNode() {
        Set<Map.Entry<Interval<T>, Long>> entries = currentNode.getIntervals().entrySet();
        Iterator<Map.Entry<Interval<T>, Long>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Interval<T>, Long> entry = iterator.next();
            Interval<T> interval = entry.getKey();

            if (interval.getStart() >= leftBorder) {
                setCurrentIntervalCount(entry.getValue());
                setIntervalsMapIterator(iterator);
                currentInterval = interval;
                break;
            }
        }
    }

    private void getLastIntervalOnCurrentNode() {
        reversedIntervals.clear();
        reversedIntervals.putAll(currentNode.getIntervals());
        Set<Map.Entry<Interval<T>, Long>> entries = reversedIntervals.entrySet();
        Iterator<Map.Entry<Interval<T>, Long>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Interval<T>, Long> entry = iterator.next();

            Interval<T> interval = entry.getKey();
            if (interval.getEnd() <= rightBorder) {
                setCurrentIntervalCount(entry.getValue());
                setIntervalsMapIterator(iterator);
                currentInterval = interval;
                break;
            }
        }
    }

}
