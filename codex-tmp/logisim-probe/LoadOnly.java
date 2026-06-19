import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;

import java.io.File;

public class LoadOnly {
    public static void main(String[] args) throws Exception {
        System.out.println("before-open");
        System.out.flush();
        Loader loader = new Loader(null);
        LogisimFile file = loader.openLogisimFile(new File(args[0]));
        System.out.println("after-open circuits=" + file.getCircuits().size()
                + " main=" + file.getMainCircuit().getName());
        System.out.flush();
        Project project = new Project(file);
        System.out.println("after-project state=" + project.getCircuitState().getCircuit().getName());
        System.out.flush();
        System.exit(0);
    }
}
