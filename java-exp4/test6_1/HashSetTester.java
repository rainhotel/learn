package test6_1;

import java.util.HashSet;
import java.util.Set;

public class HashSetTester {
    public static void main(String[] args) {
        Set<String> set = new HashSet<>(3);

        set.add("one");
        set.add("two");
        set.add("three");
        System.out.println("The initial set: " + set);

        boolean removed = set.remove("three");
        System.out.println("The element 'three' is removed from the set: " + removed);

        removed = set.remove("three");
        System.out.println("The element 'three' is removed from the set once again: " + removed);

        boolean added = set.add("three");
        System.out.println("The element 'three' is added to the set: " + added);

        added = set.add("three");
        System.out.println("The element 'three' is added to the set once again: " + added);

        Set<String> setToRetain = new HashSet<>(2);
        setToRetain.add("one");
        setToRetain.add("two");
        System.out.println("The elements to retain: " + setToRetain);

        set.retainAll(setToRetain);
        System.out.println("The set after retaining: " + set);

        Set<String> setToRemove = new HashSet<>(2);
        setToRemove.add("two");
        setToRemove.add("three");
        System.out.println("The elements to remove: " + setToRemove);

        set.removeAll(setToRemove);
        System.out.println("The set after removing: " + set);

        set.clear();
        System.out.println("The set is empty after clearing: " + set.isEmpty());

        try {
            set.add(null);
        } catch (Exception e) {
            System.out.println("The set does not allow 'null' element!");
        }

        System.out.println("The set now contains a 'null' element: " + set.contains(null));
        System.out.println("202483290054 李林浩");
    }
}
