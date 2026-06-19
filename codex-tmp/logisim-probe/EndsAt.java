import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;

import java.io.File;
import java.lang.reflect.Method;

public class EndsAt {
    private static String invoke(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            m.setAccessible(true);
            return String.valueOf(m.invoke(obj));
        } catch (Exception e) {
            return "?";
        }
    }

    public static void main(String[] args) throws Exception {
        Loader loader = new Loader(null);
        LogisimFile file = loader.openLogisimFile(new File(args[0]));
        String circuitName = args[1];
        int x = Integer.parseInt(args[2]);
        int y = Integer.parseInt(args[3]);
        Circuit circuit = null;
        for (Circuit c : file.getCircuits()) {
            if (c.getName().equals(circuitName)) {
                circuit = c;
                break;
            }
        }
        if (circuit == null) throw new IllegalArgumentException("No circuit " + circuitName);
        Location target = Location.create(x, y);
        for (Component comp : circuit.getNonWires()) {
            if (!target.equals(comp.getLocation())) continue;
            System.out.println("COMP " + comp.getFactory() + " loc=" + comp.getLocation());
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
        }
        System.exit(0);
    }
}
