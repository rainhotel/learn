import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

public class DumpEnds {
    private static String invoke(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            m.setAccessible(true);
            Object ret = m.invoke(obj);
            return String.valueOf(ret);
        } catch (Exception e) {
            return "?";
        }
    }

    private static boolean interesting(Component comp) {
        String factory = String.valueOf(comp.getFactory());
        String text = factory + " " + comp.getLocation();
        return text.contains("硬布线控制器")
                || text.contains("时序发生器")
                || text.contains("Register")
                || text.contains("ROM")
                || text.contains("Pin")
                || text.contains("Splitter")
                || text.contains("Constant")
                || text.contains("Tunnel");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: DumpEnds file.circ circuit-name");
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
        if (circuit == null) {
            throw new IllegalArgumentException("No circuit named " + args[1]);
        }

        for (Component comp : circuit.getNonWires()) {
            if (!interesting(comp)) continue;
            Location loc = comp.getLocation();
            System.out.println("COMP " + comp.getFactory() + " loc=" + loc);
            int index = 0;
            for (EndData end : comp.getEnds()) {
                System.out.println("  end" + index
                        + " loc=" + end.getLocation()
                        + " width=" + end.getWidth()
                        + " type=" + invoke(end, "getType")
                        + " input=" + invoke(end, "isInput")
                        + " output=" + invoke(end, "isOutput"));
                index++;
            }
            Object attrs = comp.getAttributeSet();
            System.out.println("  attrs=" + attrs);
        }
    }
}
