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
import java.lang.reflect.Method;

public class QuickSim {
    private static Component at(Circuit circuit, int x, int y) {
        Location loc = Location.create(x, y);
        for (Component comp : circuit.getNonWires()) {
            if (loc.equals(comp.getLocation())) return comp;
        }
        return null;
    }

    private static int ramGet(CircuitState state, Circuit circuit, int ramX, int ramY, long addr) throws Exception {
        Component ram = at(circuit, ramX, ramY);
        Object ramState = state.getData(ram);
        Method getContents = ramState.getClass().getSuperclass().getDeclaredMethod("getContents");
        getContents.setAccessible(true);
        Object contents = getContents.invoke(ramState);
        Method get = contents.getClass().getDeclaredMethod("get", long.class);
        get.setAccessible(true);
        return (Integer) get.invoke(contents, addr);
    }

    private static String valueAt(CircuitState state, int x, int y) {
        Value value = state.getValue(Location.create(x, y));
        return value == null ? "<null>" : value.toHexString();
    }

    public static void main(String[] args) throws Exception {
        Loader loader = new Loader(null);
        LogisimFile file = loader.openLogisimFile(new File(args[0]));
        Project project = new Project(file);
        Circuit circuit = file.getMainCircuit();
        CircuitState state = project.getCircuitState();
        Propagator prop = state.getPropagator();
        prop.propagate();
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        for (int i = 0; i <= ticks; i++) {
            if (i > 0) {
                prop.tick();
                prop.propagate();
            }
            System.out.printf("t=%d ctrl=%s rom=%s mif=%s t1=%s d=%s mem80=%08x%n",
                    i,
                    valueAt(state, 900, 100),
                    valueAt(state, 540, 310),
                    valueAt(state, 480, 360),
                    valueAt(state, 490, 470),
                    valueAt(state, 170, 410),
                    ramGet(state, circuit, 350, 280, 0x80));
        }
        System.exit(0);
    }
}
