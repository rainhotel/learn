import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HashMapLab {
    public static void main(String[] args) throws Exception {
        experimentEqualsHashCodeContract();
        experimentMutableKey();
        experimentTreeificationThreshold();
        experimentExpectedMappingsFactory();
        experimentFailFastIterator();
        System.out.println("ALL_EXPERIMENTS_PASSED");
    }

    private static void experimentEqualsHashCodeContract() {
        System.out.println("\n=== Experiment 1: equals/hashCode contract ===");
        BrokenKey first = new BrokenKey("tenant-1");
        BrokenKey logicallyEqual = new BrokenKey("tenant-1");
        Map<BrokenKey, String> map = new HashMap<>();

        map.put(first, "template-A");
        String lookup = map.get(logicallyEqual);
        map.put(logicallyEqual, "template-B");

        System.out.printf(
                "equals=%s hash1=%d hash2=%d lookup=%s size=%d%n",
                first.equals(logicallyEqual),
                first.hashCode(),
                logicallyEqual.hashCode(),
                lookup,
                map.size());

        check(first.equals(logicallyEqual), "keys should be logically equal");
        check(first.hashCode() != logicallyEqual.hashCode(), "lab needs different identity hash codes");
        check(lookup == null, "lookup should fail when equal keys have different hashes");
        check(map.size() == 2, "broken contract can create two logically equal entries");
    }

    private static void experimentMutableKey() {
        System.out.println("\n=== Experiment 2: mutable key ===");
        MutableKey key = new MutableKey("before");
        Map<MutableKey, String> map = new HashMap<>();
        map.put(key, "notification-task");

        key.id = "after";
        String lookupAfterMutation = map.get(key);
        boolean entryStillPresentDuringIteration = map.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == key);

        System.out.printf(
                "lookup-after-mutation=%s entry-visible-by-iteration=%s size=%d%n",
                lookupAfterMutation,
                entryStillPresentDuringIteration,
                map.size());

        check(lookupAfterMutation == null, "mutating a hash-relevant field should break lookup in this setup");
        check(entryStillPresentDuringIteration, "entry still physically exists in the table");
    }

    private static void experimentTreeificationThreshold() throws Exception {
        System.out.println("\n=== Experiment 3: collision bin treeification ===");
        HashMap<CollidingKey, Integer> map = HashMap.newHashMap(100);

        for (int i = 1; i <= 8; i++) {
            map.put(new CollidingKey(i), i);
        }
        String classWithEightEntries = firstNonNullBinClass(map);

        map.put(new CollidingKey(9), 9);
        String classWithNineEntries = firstNonNullBinClass(map);

        System.out.printf(
                "8-collisions=%s 9-collisions=%s table-capacity=%d%n",
                classWithEightEntries,
                classWithNineEntries,
                tableCapacity(map));

        check(classWithEightEntries.endsWith("HashMap$Node"), "eight entries should still form a list in JDK 21");
        check(classWithNineEntries.endsWith("HashMap$TreeNode"), "ninth entry should treeify when capacity is sufficient");
    }

    private static void experimentExpectedMappingsFactory() throws Exception {
        System.out.println("\n=== Experiment 4: initial capacity versus expected mappings ===");
        HashMap<Integer, Integer> constructorMap = new HashMap<>(100);
        HashMap<Integer, Integer> factoryMap = HashMap.newHashMap(100);

        for (int i = 0; i < 96; i++) {
            constructorMap.put(i, i);
            factoryMap.put(i, i);
        }

        int constructorCapacityAt96 = tableCapacity(constructorMap);
        int factoryCapacityAt96 = tableCapacity(factoryMap);

        constructorMap.put(96, 96);
        factoryMap.put(96, 96);

        int constructorCapacityAt97 = tableCapacity(constructorMap);
        int factoryCapacityAt97 = tableCapacity(factoryMap);

        System.out.printf(
                "constructor capacity 96->97: %d->%d; factory capacity 96->97: %d->%d%n",
                constructorCapacityAt96,
                constructorCapacityAt97,
                factoryCapacityAt96,
                factoryCapacityAt97);

        check(constructorCapacityAt96 == 128, "constructor should initially allocate capacity 128");
        check(constructorCapacityAt97 == 256, "constructor map should resize after crossing threshold 96");
        check(factoryCapacityAt96 == 256, "factory should initially allocate for 100 expected mappings");
        check(factoryCapacityAt97 == 256, "factory should not resize at 97 mappings");
    }

    private static void experimentFailFastIterator() {
        System.out.println("\n=== Experiment 5: fail-fast iterator ===");
        Map<String, Integer> map = new HashMap<>();
        map.put("sms", 1);
        map.put("email", 2);

        Iterator<String> iterator = map.keySet().iterator();
        iterator.next();
        map.put("in-app", 3);

        boolean concurrentModificationObserved = false;
        try {
            iterator.next();
        } catch (ConcurrentModificationException expected) {
            concurrentModificationObserved = true;
            System.out.println("ConcurrentModificationException observed");
        }

        check(concurrentModificationObserved, "single-threaded structural modification should be detected here");
    }

    private static int tableCapacity(HashMap<?, ?> map) throws Exception {
        Object[] table = table(map);
        return table == null ? 0 : table.length;
    }

    private static String firstNonNullBinClass(HashMap<?, ?> map) throws Exception {
        Object[] table = table(map);
        if (table != null) {
            for (Object bin : table) {
                if (bin != null) {
                    return bin.getClass().getName();
                }
            }
        }
        throw new AssertionError("no populated bin found");
    }

    private static Object[] table(HashMap<?, ?> map) throws Exception {
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        return (Object[]) tableField.get(map);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class BrokenKey {
        private final String id;

        private BrokenKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof BrokenKey key && id.equals(key.id);
        }

        // Intentionally inherits Object.hashCode() for the experiment.
    }

    private static final class MutableKey {
        private String id;

        private MutableKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MutableKey key && id.equals(key.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    private record CollidingKey(int id) {
        @Override
        public int hashCode() {
            return 42;
        }
    }
}
