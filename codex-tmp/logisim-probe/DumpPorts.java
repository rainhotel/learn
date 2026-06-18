import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.Instance;

import java.io.File;
import java.util.Map;
import java.util.SortedMap;

public class DumpPorts {
    private static String attrs(AttributeSet set) {
        StringBuilder ret = new StringBuilder();
        for (Attribute<?> attr : set.getAttributes()) {
            Object value = set.getValue(attr);
            if (ret.length() > 0) ret.append(", ");
            ret.append(attr.getName()).append("=").append(value);
        }
        return ret.toString();
    }

    private static String label(Instance inst) {
        AttributeSet set = inst.getAttributeSet();
        for (Attribute<?> attr : set.getAttributes()) {
            if ("label".equals(attr.getName())) {
                Object value = set.getValue(attr);
                return String.valueOf(value);
            }
        }
        return "";
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: DumpPorts file.circ circuit-name");
            System.exit(2);
        }
        Loader loader = new Loader(null);
        LogisimFile file = loader.openLogisimFile(new File(args[0]));
        Circuit circuit = null;
        for (Circuit c : file.getCircuits()) {
            if (c.getName().equals(args[1])) {
                circuit = c;
                break;
            }
        }
        if (circuit == null) throw new IllegalArgumentException("No circuit named " + args[1]);

        CircuitAppearance app = circuit.getAppearance();
        System.out.println("circuit=" + circuit.getName() + " default=" + app.isDefaultAppearance());
        for (Instance pin : app.getCircuitPins().getPins()) {
            System.out.println("pin loc=" + pin.getLocation()
                    + " label=" + label(pin)
                    + " attrs={" + attrs(pin.getAttributeSet()) + "}");
        }
        for (Direction dir : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
            SortedMap<Location, Instance> ports = app.getPortOffsets(dir);
            System.out.println("direction=" + dir);
            for (Map.Entry<Location, Instance> e : ports.entrySet()) {
                Instance pin = e.getValue();
                System.out.println("  offset=" + e.getKey()
                        + " pinLoc=" + pin.getLocation()
                        + " label=" + label(pin)
                        + " attrs={" + attrs(pin.getAttributeSet()) + "}");
            }
        }
    }
}
