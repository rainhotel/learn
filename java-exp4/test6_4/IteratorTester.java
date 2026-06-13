package test6_4;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

public class IteratorTester {
    public static void main(String[] args) {
        String[] num = {
            "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "ten"
        };

        Vector<String> vector = new Vector<>(Arrays.asList(num));
        System.out.println("The initial Vector is: " + vector);

        Iterator<String> nums = vector.iterator();
        while (nums.hasNext()) {
            String aString = nums.next();
            System.out.println(aString);
            if (aString.length() > 4) {
                nums.remove();
            }
        }

        System.out.println("The Vector after iteration is: " + vector);
        System.out.println("202483290054 李林浩");
    }
}
