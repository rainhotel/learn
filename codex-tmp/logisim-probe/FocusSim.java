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
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FocusSim {
    private record Point(String name, int x, int y) {}

    private static final Point[] CONTROL = {
            new Point("PCout", 380, 1000),
            new Point("DRout", 380, 1060),
            new Point("Zout", 420, 1000),
            new Point("Rout", 420, 1060),
            new Point("IRiout", 460, 1000),
            new Point("IRaout", 460, 1060),
            new Point("DREout", 500, 1000),
            new Point("PCin", 500, 1060),
            new Point("ARin", 540, 1000),
            new Point("DREin", 540, 1060),
            new Point("DRin", 580, 1000),
            new Point("Xin", 580, 1060),
            new Point("Rin", 610, 1000),
            new Point("IRin", 610, 1060),
            new Point("PSWin", 650, 1000),
            new Point("RegTgt", 650, 1060),
            new Point("RegDst", 690, 1000),
            new Point("Add", 700, 1060),
            new Point("Add4", 730, 1000),
            new Point("Slt", 730, 1060),
            new Point("READ", 770, 1000),
            new Point("Write", 770, 1060)
    };

    private static final Point[] LOCAL_CONTROL = {
            new Point("PCout@", 540, 120),
            new Point("PCin@", 400, 170),
            new Point("ARin@", 90, 310),
            new Point("DRout@", 540, 260),
            new Point("DRin@", 410, 240),
            new Point("DREout@", 400, 370),
            new Point("DREin@", 400, 300),
            new Point("Xin@", 150, 530),
            new Point("Add4@", 320, 600),
            new Point("IRin@", 630, 240),
            new Point("READ@", 310, 340),
            new Point("Write@", 230, 340)
    };

    private static final Point[] DATA_POINTS = {
            new Point("bus", 90, 210),
            new Point("arQ", 150, 280),
            new Point("arSplit", 170, 280),
            new Point("dbgAR", 190, 280),
            new Point("ramA", 210, 280),
            new Point("ramDin", 210, 300),
            new Point("ramD", 350, 280),
            new Point("pcQ", 490, 140),
            new Point("pcOutBus", 590, 140)
    };

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

    private static int ramGet(CircuitState state, Circuit circuit, int ramX, int ramY, long addr) throws Exception {
        Component ram = at(circuit, ramX, ramY);
        if (ram == null) return 0;
        Object ramState = state.getData(ram);
        if (ramState == null) return 0;
        Method getContents = ramState.getClass().getSuperclass().getDeclaredMethod("getContents");
        getContents.setAccessible(true);
        Object contents = getContents.invoke(ramState);
        Method get = contents.getClass().getDeclaredMethod("get", long.class);
        get.setAccessible(true);
        return (Integer) get.invoke(contents, addr);
    }

    private static void ramSet(CircuitState state, Circuit circuit, int ramX, int ramY, long addr, int value) throws Exception {
        Component ram = at(circuit, ramX, ramY);
        if (ram == null) throw new IllegalStateException("No RAM at " + ramX + "," + ramY);
        Object ramState = state.getData(ram);
        Method getContents = ramState.getClass().getSuperclass().getDeclaredMethod("getContents");
        getContents.setAccessible(true);
        Object contents = getContents.invoke(ramState);
        Method set = contents.getClass().getDeclaredMethod("set", long.class, int.class);
        set.setAccessible(true);
        set.invoke(contents, addr, value);
    }

    private static void loadRawHex(CircuitState state, Circuit circuit, File hexFile) throws Exception {
        List<Integer> words = new ArrayList<>();
        for (String line : Files.readAllLines(hexFile.toPath())) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("v2.0")) continue;
            for (String token : trimmed.split("\\s+")) {
                if (!token.isEmpty()) {
                    words.add((int) Long.parseUnsignedLong(token, 16));
                }
            }
        }
        for (int i = 0; i < words.size(); i++) {
            ramSet(state, circuit, 350, 280, i, words.get(i));
        }
        System.out.printf("loaded %d words from %s%n", words.size(), hexFile);
    }

    private static String hex(Integer value) {
        return value == null ? "........" : String.format("%08x", value);
    }

    private static String ports(CircuitState state, Circuit circuit, int x, int y) {
        Component comp = at(circuit, x, y);
        if (comp == null) return "missing";
        StringBuilder sb = new StringBuilder();
        var inst = state.getInstanceState(comp);
        for (int i = 0; i < comp.getEnds().size(); i++) {
            sb.append(" p").append(i).append('=').append(inst.getPort(i).toHexString());
        }
        return sb.toString();
    }

    private static CircuitState substateNamed(CircuitState state, String namePart) {
        for (CircuitState sub : state.getSubstates()) {
            if (sub.getCircuit().getName().contains(namePart)) return sub;
            CircuitState nested = substateNamed(sub, namePart);
            if (nested != null) return nested;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: FocusSim file.circ [ticks]");
            System.exit(2);
        }
        int ticks = args.length >= 2 ? Integer.parseInt(args[1]) : 20;
        boolean resetFirst = false;
        File hexFile = null;
        for (int i = 2; i < args.length; i++) {
            if ("reset".equalsIgnoreCase(args[i])) {
                resetFirst = true;
            } else {
                hexFile = new File(args[i]);
            }
        }
        Loader loader = new Loader(null);
        LogisimFile file = loader.openLogisimFile(new File(args[0]));
        Project project = new Project(file);
        if (resetFirst) {
            project.getSimulator().requestReset();
        }
        Circuit circuit = file.getMainCircuit();
        CircuitState state = project.getCircuitState();
        Propagator prop = state.getPropagator();
        prop.propagate();
        if (hexFile != null) {
            loadRawHex(state, circuit, hexFile);
            prop.propagate();
        }

        for (int tick = 0; tick <= ticks; tick++) {
            if (tick > 0) prop.tick();
            prop.propagate();
            StringBuilder controls = new StringBuilder();
            for (Point p : CONTROL) {
                String v = valueAt(state, p.x, p.y);
                if (!"0".equals(v)) controls.append(' ').append(p.name).append('=').append(v);
            }
            for (Point p : LOCAL_CONTROL) {
                String v = valueAt(state, p.x, p.y);
                if (!"0".equals(v)) controls.append(' ').append(p.name).append('=').append(v);
            }
            for (Point p : DATA_POINTS) {
                String v = valueAt(state, p.x, p.y);
                if (v.contains("x") || v.contains("E") || tick <= 2) {
                    controls.append(' ').append(p.name).append('=').append(v);
                }
            }
            CircuitState ctrlState = substateNamed(state, "硬布线控制器");
            if (ctrlState != null) {
                controls.append(" oc=").append(valueAt(ctrlState, 790, 350))
                        .append(" mif=").append(valueAt(ctrlState, 940, 340))
                        .append(" mcal=").append(valueAt(ctrlState, 900, 350))
                        .append(" mex=").append(valueAt(ctrlState, 860, 360))
                        .append(" t1=").append(valueAt(ctrlState, 860, 430))
                        .append(" t2=").append(valueAt(ctrlState, 900, 430))
                        .append(" t3=").append(valueAt(ctrlState, 940, 430))
                        .append(" t4=").append(valueAt(ctrlState, 980, 430))
                        .append(" stateRomIn=").append(valueAt(ctrlState, 280, 330))
                        .append(" stateRomOut=").append(valueAt(ctrlState, 420, 330))
                        .append(" outRomAddr=").append(valueAt(ctrlState, 400, 310))
                        .append(" outRom=").append(valueAt(ctrlState, 540, 310))
                        .append(" ctrlIRpin=").append(valueAt(ctrlState, 140, 80))
                        .append(" decIn=").append(valueAt(ctrlState, 900, 120))
                        .append(" decLW=").append(valueAt(ctrlState, 980, 100))
                        .append(" decSW=").append(valueAt(ctrlState, 1020, 110))
                        .append(" decBEQ=").append(valueAt(ctrlState, 1060, 120))
                        .append(" decADDI=").append(valueAt(ctrlState, 1110, 130))
                        .append(" decSLT=").append(valueAt(ctrlState, 1150, 140))
                        .append(" decOther=").append(valueAt(ctrlState, 1190, 150))
                        .append(" ocSLT=").append(valueAt(ctrlState, 830, 290))
                        .append(" ocADDI=").append(valueAt(ctrlState, 830, 300))
                        .append(" ocLW=").append(valueAt(ctrlState, 830, 310))
                        .append(" ocSW=").append(valueAt(ctrlState, 830, 320))
                        .append(" ocBEQ=").append(valueAt(ctrlState, 830, 330))
                        .append(" rawT1=").append(valueAt(ctrlState, 550, 410))
                        .append(" rawT2=").append(valueAt(ctrlState, 550, 420))
                        .append(" rawT3=").append(valueAt(ctrlState, 550, 430))
                        .append(" rawT4=").append(valueAt(ctrlState, 550, 440))
                        .append(" ocT1=").append(valueAt(ctrlState, 830, 370))
                        .append(" ocT2=").append(valueAt(ctrlState, 830, 380))
                        .append(" ocT3=").append(valueAt(ctrlState, 830, 390))
                        .append(" ocT4=").append(valueAt(ctrlState, 830, 400))
                        .append(" srcT3=").append(valueAt(ctrlState, 550, 430))
                        .append(" srcT4=").append(valueAt(ctrlState, 550, 440));
            }
            System.out.printf(
                    "tick=%03d ctrl=%s pc=%s ir=%s ar=%s ramAddr=%s WE=%s OE=%s DOUT=%s m80=%08x m81=%08x%s%n",
                    tick,
                    valueAt(state, 900, 100),
                    hex(registerValue(state, circuit, 490, 140)),
                    hex(registerValue(state, circuit, 690, 210)),
                    hex(registerValue(state, circuit, 150, 280)),
                    valueAt(state, 210, 280),
                    valueAt(state, 240, 320),
                    valueAt(state, 300, 320),
                    valueAt(state, 350, 280),
                    ramGet(state, circuit, 350, 280, 0x80),
                    ramGet(state, circuit, 350, 280, 0x81),
                    controls
            );
            if (tick <= 3 || (tick >= 22 && tick <= 27)) {
                System.out.println("  PC ports:" + ports(state, circuit, 490, 140));
                System.out.println("  AR ports:" + ports(state, circuit, 150, 280));
                System.out.println("  X ports:" + ports(state, circuit, 210, 500));
            }
        }
        System.exit(0);
    }
}
