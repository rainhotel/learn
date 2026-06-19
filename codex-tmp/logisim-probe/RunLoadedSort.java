import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Propagator;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunLoadedSort {
    private static Component at(Circuit circuit, int x, int y) {
        Location loc = Location.create(x, y);
        for (Component comp : circuit.getNonWires()) {
            if (loc.equals(comp.getLocation())) return comp;
        }
        return null;
    }

    private static String valueAt(CircuitState state, int x, int y) {
        Value value = state.getValue(Location.create(x, y));
        return value == null ? "<null>" : value.toHexString();
    }

    private static Integer registerValue(CircuitState state, Circuit circuit, int x, int y) throws Exception {
        Component comp = at(circuit, x, y);
        if (comp == null) return null;
        Object data = state.getData(comp);
        if (data == null) return null;
        try {
            Method m = data.getClass().getDeclaredMethod("getValue");
            m.setAccessible(true);
            Object ret = m.invoke(data);
            return ret instanceof Integer ? (Integer) ret : null;
        } catch (NoSuchMethodException ignored) {
            for (String fieldName : new String[]{"value", "curValue", "val"}) {
                try {
                    Field f = data.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object ret = f.get(data);
                    return ret instanceof Integer ? (Integer) ret : null;
                } catch (NoSuchFieldException ignored2) {
                    // Try the next common field name.
                }
            }
            return null;
        }
    }

    private static Object ramContents(CircuitState state, Circuit circuit) throws Exception {
        Component ram = at(circuit, 350, 280);
        if (ram == null) throw new IllegalStateException("No RAM at 350,280");
        Object ramState = state.getData(ram);
        if (ramState == null) throw new IllegalStateException("RAM state is null");
        Method getContents = ramState.getClass().getSuperclass().getDeclaredMethod("getContents");
        getContents.setAccessible(true);
        return getContents.invoke(ramState);
    }

    private static int ramGet(Object contents, long addr) throws Exception {
        Method get = contents.getClass().getDeclaredMethod("get", long.class);
        get.setAccessible(true);
        return (Integer) get.invoke(contents, addr);
    }

    private static void ramSet(Object contents, long addr, int value) throws Exception {
        Method set = contents.getClass().getDeclaredMethod("set", long.class, int.class);
        set.setAccessible(true);
        set.invoke(contents, addr, value);
    }

    private static void loadRawHex(Object contents, File hexFile) throws Exception {
        List<Integer> words = new ArrayList<>();
        for (String line : Files.readAllLines(hexFile.toPath())) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("v2.0")) continue;
            for (String token : trimmed.split("\\s+")) {
                if (!token.isEmpty()) words.add((int) Long.parseUnsignedLong(token, 16));
            }
        }
        for (int i = 0; i < words.size(); i++) ramSet(contents, i, words.get(i));
        System.out.printf("loaded=%d%n", words.size());
    }

    private static int[] data(Object contents) throws Exception {
        int[] ret = new int[8];
        for (int i = 0; i < ret.length; i++) ret[i] = ramGet(contents, 0x80 + i);
        return ret;
    }

    private static String hex(Integer value) {
        return value == null ? "........" : String.format("%08x", value);
    }

    private static String words(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%08x", values[i]));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: RunLoadedSort file.circ sort.hex [ticks]");
            System.exit(2);
        }
        int ticks = args.length >= 3 ? Integer.parseInt(args[2]) : 20000;
        Loader loader = new Loader(null);
        LogisimFile file = loader.openLogisimFile(new File(args[0]));
        Project project = new Project(file);
        Circuit circuit = file.getMainCircuit();
        CircuitState state = project.getCircuitState();
        Propagator prop = state.getPropagator();
        prop.propagate();
        Object contents = ramContents(state, circuit);
        loadRawHex(contents, new File(args[1]));
        prop.propagate();

        int[] last = data(contents);
        System.out.printf("initial-data=%s%n", words(last));
        for (int tick = 1; tick <= ticks; tick++) {
            prop.tick();
            prop.propagate();
            int[] cur = data(contents);
            boolean changed = !Arrays.equals(last, cur);
            boolean write = "1".equals(valueAt(state, 240, 320));
            if (changed || write) {
                System.out.printf(
                        "tick=%05d pc=%s ir=%s ar=%s ramAddr=%s WE=%s DIN=%s data=%s%n",
                        tick,
                        hex(registerValue(state, circuit, 490, 140)),
                        hex(registerValue(state, circuit, 690, 210)),
                        hex(registerValue(state, circuit, 150, 280)),
                        valueAt(state, 210, 280),
                        valueAt(state, 240, 320),
                        valueAt(state, 210, 300),
                        words(cur));
            }
            last = cur;
        }
        System.out.printf("final-data=%s%n", words(last));
        System.exit(0);
    }
}
