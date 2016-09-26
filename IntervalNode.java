package com.kg.smartfactory.common.interval.tree;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * The Node class contains the interval tree information for one single node
 *
 * @author Kevin Dolan
 */
public class IntervalNode<Type> implements Serializable {

    private SortedMap<Interval<Type>, Long> intervals;
    private long center;

    public SortedMap<Interval<Type>, Long> getIntervals() {
        return intervals;
    }

    private IntervalNode<Type> leftNode;
    private IntervalNode<Type> rightNode;
    private IntervalNode<Type> parent;

    public IntervalNode(IntervalNode<Type> parent) {
        intervals = new TreeMap<Interval<Type>, Long>();
        center = 0;
        leftNode = null;
        rightNode = null;
        this.parent = parent;
    }

    public IntervalNode(List<Interval<Type>> intervalList, IntervalNode<Type> parent) {

        intervals = new TreeMap<Interval<Type>, Long>();
        this.parent = parent;

        SortedSet<Long> endpoints = new TreeSet<Long>();

        for (Interval<Type> interval : intervalList) {
            endpoints.add(interval.getStart());
            endpoints.add(interval.getEnd());
        }

        long median = getMedian(endpoints);
        center = median;

        List<Interval<Type>> left = new ArrayList<Interval<Type>>();
        List<Interval<Type>> right = new ArrayList<Interval<Type>>();

        for (Interval<Type> interval : intervalList) {
            if (interval.getEnd() < median)
                left.add(interval);
            else if (interval.getStart() > median)
                right.add(interval);
            else {
                Long count = intervals.get(interval);
                if (count == null) {
                    intervals.put(interval, 0L);
                    count = 0L;
                }
                intervals.put(interval, count + 1);
            }
        }

        if (left.size() > 0)
            leftNode = new IntervalNode<Type>(left, this);
        if (right.size() > 0)
            rightNode = new IntervalNode<Type>(right, this);
    }

    /**
     * Perform a stabbing query on the node
     *
     * @param time the time to query at
     * @return all intervals containing time
     */
    public List<Interval<Type>> stab(long time) {
        List<Interval<Type>> result = new ArrayList<Interval<Type>>();

        for (Entry<Interval<Type>, Long> entry : intervals.entrySet()) {
            if (entry.getKey().contains(time))
                for (Long i = 0L; i < entry.getValue(); i++)
                    result.add(entry.getKey());
            else if (entry.getKey().getStart() > time)
                break;
        }

        if (time < center && leftNode != null)
            result.addAll(leftNode.stab(time));
        else if (time > center && rightNode != null)
            result.addAll(rightNode.stab(time));
        return result;
    }

    /**
     * Perform an interval intersection query on the node
     *
     * @param target the interval to intersect
     * @return all intervals containing time
     */
    public List<Interval<Type>> query(Interval<?> target) {
        List<Interval<Type>> result = new ArrayList<Interval<Type>>();

        for (Entry<Interval<Type>, Long> entry : intervals.entrySet()) {
            if (entry.getKey().intersects(target))
                for (Long i = 0L; i < entry.getValue(); i++) {
                    result.add(entry.getKey());
                }
            else if (entry.getKey().getStart() > target.getEnd())
                break;
        }

        if (target.getStart() < center && leftNode != null)
            result.addAll(leftNode.query(target));
        if (target.getEnd() > center && rightNode != null)
            result.addAll(rightNode.query(target));
        return result;
    }

    public long getCenter() {
        return center;
    }

    public void setCenter(long center) {
        this.center = center;
    }

    public IntervalNode<Type> getLeft() {
        return leftNode;
    }

    public void setLeft(IntervalNode<Type> left) {
        this.leftNode = left;
    }

    public IntervalNode<Type> getRight() {
        return rightNode;
    }

    public void setRight(IntervalNode<Type> right) {
        this.rightNode = right;
    }

    /**
     * @param set the set to look on
     * @return the median of the set, not interpolated
     */
    private Long getMedian(SortedSet<Long> set) {
        int i = 0;
        int middle = set.size() / 2;
        for (Long point : set) {
            if (i == middle)
                return point;
            i++;
        }
        return 0L;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(center + ": ");
        for (Entry<Interval<Type>, Long> entry : intervals.entrySet()) {
            sb.append("[" + entry.getKey().getStart() + "," + entry.getKey().getEnd() + "]:{");
            for (Long i = 0L; i < entry.getValue(); i++) {
                sb.append("(" + entry.getKey().getStart() + "," + entry.getKey().getEnd() + "," + entry.getKey().getData() + ")");
            }
            sb.append("} ");
        }
        return sb.toString();
    }

    public IntervalNode<Type> getParent() {
        return parent;
    }
}
