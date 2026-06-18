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
import java.lang.reflect.Field;
import java.util.Arrays;

public class SimInspect {
    private static Component at(Circuit circuit, int x, int y) {
        Location loc = Location.create(x, y);
        for (Component comp : circuit.getNonWires()) {
            if (loc.equals(comp.getLocation())) {
                return comp;
            }
        }
        return null;
    }

    private static String valueAt(CircuitState state, int x, int y) {
        Value value = state.getValue(Location.create(x, y));
        if (value == null) return "<null>";
        return value.toHexString();
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

    private static int ramGet(CircuitState state, Circuit circuit, int ramX, int ramY, long addr) throws Exception {
        Component ram = at(circuit, ramX, ramY);
        if (ram == null) throw new IllegalStateException("No RAM at " + ramX + "," + ramY);
        Object ramState = state.getData(ram);
        if (ramState == null) throw new IllegalStateException("RAM state is null");
        Method getContents = ramState.getClass().getSuperclass().getDeclaredMethod("getContents");
        getContents.setAccessible(true);
        Object contents = getContents.invoke(ramState);
        Method get = contents.getClass().getDeclaredMethod("get", long.class);
        get.setAccessible(true);
        return (Integer) get.invoke(contents, addr);
    }

    private static String hex(Integer value) {
        if (value == null) return "........";
        return String.format("%08x", value);
    }

    private static void printLine(int tick, CircuitState state, Circuit circuit) throws Exception {
        Integer pc = registerValue(state, circuit, 490, 140);
        Integer ir = registerValue(state, circuit, 690, 210);
        Integer ar = registerValue(state, circuit, 150, 280);
        Integer dr = registerValue(state, circuit, 490, 280);
        Integer x = registerValue(state, circuit, 210, 500);
        Integer z = registerValue(state, circuit, 490, 550);
        int m80 = ramGet(state, circuit, 350, 280, 0x80);
        int m81 = ramGet(state, circuit, 350, 280, 0x81);
        int m200 = ramGet(state, circuit, 350, 280, 0x200);
        System.out.printf(
                "tick=%04d pc=%s ir=%s ar=%s dr=%s x=%s z=%s ramAddr=%s WE=%s OE=%s DIN=%s DOUT=%s mem80=%08x mem81=%08x mem200=%08x ctrl=%s state=%s%n",
                tick,
                hex(pc),
                hex(ir),
                hex(ar),
                hex(dr),
                hex(x),
                hex(z),
                valueAt(state, 210, 280),
                valueAt(state, 240, 320),
                valueAt(state, 300, 320),
                valueAt(state, 210, 300),
                valueAt(state, 350, 280),
                m80,
                m81,
                m200,
                valueAt(state, 900, 100),
                valueAt(state, 800, 140)
        );
    }

    private static void dumpStateTree(CircuitState state, String indent) throws Exception {
        Circuit circuit = state.getCircuit();
        System.out.println(indent + "circuit=" + circuit.getName());
        for (Component comp : circuit.getNonWires()) {
            String factory = String.valueOf(comp.getFactory());
            if (factory.contains("Register")) {
                Location loc = comp.getLocation();
                Integer value = registerValue(state, circuit, loc.getX(), loc.getY());
                System.out.println(indent + "  reg " + loc + " value=" + hex(value)
                        + " q=" + valueAt(state, loc.getX(), loc.getY()));
                if (circuit.getName().contains("硬布线控制器") && loc.getX() == 200 && loc.getY() == 410) {
                    var inst = state.getInstanceState(comp);
                    StringBuilder ports = new StringBuilder();
                    for (int i = 0; i <= 6; i++) {
                        ports.append(" p").append(i).append("=").append(inst.getPort(i).toHexString());
                    }
                    System.out.println(indent + "    ports" + ports);
                }
            } else if (circuit.getName().contains("硬布线控制器")
                    && factory.contains("时序发生器状态机")) {
                var inst = state.getInstanceState(comp);
                StringBuilder ports = new StringBuilder();
                for (int i = 0; i <= 7; i++) {
                    ports.append(" p").append(i).append("=").append(inst.getPort(i).toHexString());
                }
                System.out.println(indent + "  state-machine ports" + ports);
            } else if (circuit.getName().contains("硬布线控制器")
                    && factory.contains("时序发生器输出函数")) {
                var inst = state.getInstanceState(comp);
                StringBuilder ports = new StringBuilder();
                for (int i = 0; i <= 10; i++) {
                    ports.append(" p").append(i).append("=").append(inst.getPort(i).toHexString());
                }
                System.out.println(indent + "  timing-output ports" + ports);
            } else if ((circuit.getName().contains("时序发生器")
                    || circuit.getName().contains("硬布线控制器"))
                    && factory.contains("ROM")) {
                var inst = state.getInstanceState(comp);
                StringBuilder ports = new StringBuilder();
                for (int i = 0; i < comp.getEnds().size(); i++) {
                    ports.append(" p").append(i).append("=").append(inst.getPort(i).toHexString());
                }
                System.out.println(indent + "  rom ports" + ports);
            }
        }
        if (circuit.getName().contains("硬布线控制器")) {
            int[][] points = {
                    {290, 60}, {330, 60}, {520, 50}, {520, 110},
                    {200, 410}, {170, 410}, {170, 400}, {170, 420},
                    {180, 430}, {190, 430}, {190, 390}, {170, 430},
                    {180, 460}, {250, 250},
                    {270, 330}, {290, 310}, {290, 320}, {290, 330}, {290, 340},
                    {300, 310}, {300, 320}, {300, 330}, {300, 340},
                    {320, 310}, {320, 320}, {320, 330}, {320, 340},
                    {330, 310}, {330, 320}, {330, 330}, {330, 340},
                    {350, 310}, {350, 320}, {350, 330}, {350, 340}, {370, 330},
                    {120, 410}, {120, 450}, {380, 450},
                    {540, 310}, {570, 310}, {570, 410},
                    {550, 380}, {550, 390}, {550, 400}, {550, 410},
                    {550, 420}, {550, 430}, {550, 440},
                    {610, 380}, {610, 390}, {610, 400}, {610, 410},
                    {610, 420}, {610, 430}, {610, 440},
                    {480, 360}, {520, 360}, {560, 360}, {630, 360}
            };
            for (int[] pt : points) {
                System.out.println(indent + "  v(" + pt[0] + "," + pt[1] + ")=" + valueAt(state, pt[0], pt[1]));
            }
        }
        if (circuit.getName().contains("时序发生器")) {
            for (Component comp : circuit.getNonWires()) {
                Location loc = comp.getLocation();
                System.out.println(indent + "  comp " + factoryName(comp) + " " + loc
                        + " v=" + valueAt(state, loc.getX(), loc.getY()));
            }
        }
        for (CircuitState sub : state.getSubstates()) {
            dumpStateTree(sub, indent + "  ");
        }
    }

    private static String factoryName(Component comp) {
        Object factory = comp.getFactory();
        if (factory == null) return "<null>";
        return String.valueOf(factory);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: SimInspect file.circ [ticks] [printEvery]");
            System.exit(2);
        }
        int ticks = args.length >= 2 ? Integer.parseInt(args[1]) : 200;
        int printEvery = args.length >= 3 ? Integer.parseInt(args[2]) : 1;

        Loader loader = new Loader(null);
        LogisimFile file = loader.openLogisimFile(new File(args[0]));
        Project project = new Project(file);
        Circuit circuit = file.getMainCircuit();
        CircuitState state = project.getCircuitState();
        Propagator prop = state.getPropagator();

        prop.propagate();
        System.out.println("main=" + circuit.getName());
        System.out.println("args=" + Arrays.toString(args));
        dumpStateTree(state, "");
        printLine(0, state, circuit);

        for (int i = 1; i <= ticks; i++) {
            prop.tick();
            prop.propagate();
            if (i % printEvery == 0 || i <= 20) {
                printLine(i, state, circuit);
            }
            if (ramGet(state, circuit, 350, 280, 0x80) != 0 || ramGet(state, circuit, 350, 280, 0x81) != 0) {
                System.out.println("RAM 0x80 changed at tick " + i);
                printLine(i, state, circuit);
                break;
            }
        }
        System.exit(0);
    }
}
