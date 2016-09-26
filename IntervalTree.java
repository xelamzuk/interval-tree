package com.kg.smartfactory.common.interval.tree;

import java.io.Serializable;
import java.util.*;

/**
 * An Interval Tree is essentially a map from intervals to objects, which
 * can be queried for all data associated with a particular interval of
 * time
 *
 * @param <Type> the type of objects to associate
 * @author Kevin Dolan
 */
public class IntervalTree<Type> implements Serializable {

    private IntervalNode<Type> head;
    private List<Interval<Type>> intervalList;
    private boolean inSync;
    private int size;

    /**
     * Instantiate a new interval tree with no intervals
     */
    public IntervalTree() {
        this.head = new IntervalNode<Type>(null);
        this.intervalList = new ArrayList<Interval<Type>>();
        this.inSync = true;
        this.size = 0;
    }

    /**
     * Instantiate and build an interval tree with a preset list of intervals
     *
     * @param intervalList the list of intervals to use
     */
    public IntervalTree(List<Interval<Type>> intervalList) {
        this.head = new IntervalNode<Type>(intervalList, null);
        this.intervalList = new ArrayList<Interval<Type>>();
        this.intervalList.addAll(intervalList);
        this.inSync = true;
        this.size = intervalList.size();
    }

    /**
     * Perform a stabbing query, returning the associated data
     * Will rebuild the tree if out of sync
     *
     * @param time the time to stab
     * @return the data associated with all intervals that contain time
     */
    public List<Type> get(long time) {
        List<Interval<Type>> intervals = getIntervals(time);
        List<Type> result = new ArrayList<Type>();
        for (Interval<Type> interval : intervals)
            result.add(interval.getData());
        return result;
    }

    /**
     * Perform a stabbing query, returning the interval objects
     * Will rebuild the tree if out of sync
     *
     * @param time the time to stab
     * @return all intervals that contain time
     */
    public List<Interval<Type>> getIntervals(long time) {
        build();
        return head.stab(time);
    }

    /**
     * Perform an interval query, returning the associated data
     * Will rebuild the tree if out of sync
     *
     * @param start the start of the interval to check
     * @param end   the end of the interval to check
     * @return the data associated with all intervals that intersect target
     */
    public List<Type> get(long start, long end) {
        List<Interval<Type>> intervals = getIntervals(start, end);
        List<Type> result = new ArrayList<Type>();
        for (Interval<Type> interval : intervals)
            result.add(interval.getData());
        return result;
    }

    /**
     * Perform an interval query, returning the associated data
     * Will rebuild the tree if out of sync
     *
     * @param start the start of the interval to check
     * @param end   the end of the interval to check
     * @return the data associated with all intervals that intersect target
     */
    public List<Type> get(Date start, Date end) {
        return get(start.getTime(), end.getTime());
    }

    /**
     * Perform an interval query, returning the interval objects
     * Will rebuild the tree if out of sync
     *
     * @param start the start of the interval to check
     * @param end   the end of the interval to check
     * @return all intervals that intersect target
     */
    public List<Interval<Type>> getIntervals(long start, long end) {
        build();
        return head.query(new Interval<Type>(start, end, null));
    }

    /**
     * Perform an interval query, returning the interval objects
     * Will rebuild the tree if out of sync
     *
     * @param start the start of the interval to check
     * @param end   the end of the interval to check
     * @return all intervals that intersect target
     */
    public List<Interval<Type>> getIntervals(Date start, Date end) {
        return getIntervals(start.getTime(), end.getTime());
    }

    /**
     * Add an interval object to the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     *
     * @param interval the interval object to add
     */
    public void addInterval(Interval<Type> interval) {
        intervalList.add(interval);
        inSync = false;
    }

    /**
     * Add an interval object to the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     *
     * @param begin the beginning of the interval
     * @param end   the end of the interval
     * @param data  the data to associate
     */
    public void addInterval(long begin, long end, Type data) {
        intervalList.add(new Interval<Type>(begin, end, data));
        inSync = false;
    }

    /**
     * Add an interval object to the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     *
     * @param begin the beginning of the interval
     * @param end   the end of the interval
     * @param data  the data to associate
     */
    public void addInterval(Date begin, Date end, Type data) {
        addInterval(begin.getTime(), end.getTime(), data);
    }

    /**
     * Remove an interval object from the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     *
     * @param interval the interval object to remove
     */
    public void removeInterval(Interval<Type> interval) {
        intervalList.remove(interval);
        inSync = false;
    }

    /**
     * Remove an interval object from the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     *
     * @param intervals the collection of interval object to remove
     */
    public void removeIntervals(Collection<Interval<Type>> intervals) {
        for (Interval<Type> interval : intervals) {
            intervalList.remove(interval);
        }

        inSync = false;
    }

    /**
     * Remove all an interval object from the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     */
    public void clear() {
        intervalList.clear();
        inSync = false;
    }

    public Iterator<Interval<Type>> iterator() {
        return intervalList.iterator();
    }

    /**
     * Determine whether this interval tree is currently a reflection of all intervals in the interval list
     *
     * @return true if no changes have been made since the last build
     */
    public boolean inSync() {
        return inSync;
    }

    /**
     * Build the interval tree to reflect the list of intervals,
     * Will not run if this is currently in sync
     */
    public void build() {
        if (!inSync) {
            head = new IntervalNode<Type>(intervalList, null);
            inSync = true;
            size = intervalList.size();
        }
    }

    /**
     * @return the number of entries in the currently built interval tree
     */
    public int currentSize() {
        return intervalList.size();
    }

    /**
     * @return the number of entries in the interval list, equal to .size() if inSync()
     */
    public int listSize() {
        return intervalList.size();
    }

    @Override
    public String toString() {
        return nodeString(head, 0);
    }

    private String nodeString(IntervalNode<Type> node, int level) {
        if (node == null)
            return "";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < level; i++)
            sb.append("\t");
        sb.append(node + "\n");
        sb.append(nodeString(node.getLeft(), level + 1));
        sb.append(nodeString(node.getRight(), level + 1));
        return sb.toString();
    }

    public IntervalTreeIterator<Type> getIterator(long border, IteratorDirection direction) {
        build();
        return new IntervalTreeIterator<Type>(border, head, direction);
    }

    public IntervalTreeIterator<Type> getIterator(Date border, IteratorDirection direction) {
        return getIterator(border.getTime(), direction);
    }

}
