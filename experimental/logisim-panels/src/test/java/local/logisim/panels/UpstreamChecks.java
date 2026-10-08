package local.logisim.panels;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/** Exercises availability and .circ serialization of components added after 5.0.0. */
public final class UpstreamChecks {
  private static int checks;
  private static void require(boolean value, String message) {
    checks++;
    if (!value) throw new AssertionError(message);
    System.out.println("OK " + message);
  }
  public static void main(String[] args) throws Exception {
    Project project=Checks.demo();
    Project loaded=null;
    try {
      CircuitMutation mutation=new CircuitMutation(project.getCurrentCircuit());
      int x=100;
      for (String id : new String[]{"74148","744060","74123","74390"}) {
        var tool=ModernWorkspace.findTool(project.getLogisimFile(),id,new HashSet<>());
        require(tool instanceof AddTool,"Current TTL library includes " + id);
        var factory=((AddTool)tool).getFactory();
        var component=factory.createComponent(Location.create(x,700,true),factory.createAttributeSet());
        mutation.add(component);
        x+=250;
        require(component.getFactory().getClass().getSimpleName().equals("Ttl"+id),"Native component factory instantiated: " + id);
      }
      mutation.execute();
      Path file=Path.of(args[0],"current-ttl.circ");
      try (OutputStream out=Files.newOutputStream(file)) {
        project.getLogisimFile().write(out,project.getLogisimFile().getLoader(),file.toFile());
      }
      loaded=new Project(new Loader(null).openLogisimFile(file.toFile()));
      for (String id : new String[]{"74148","744060","74123","74390"}) {
        final String type="Ttl"+id;
        require(loaded.getCurrentCircuit().getNonWires().stream().anyMatch(c -> c.getFactory().getClass().getSimpleName().equals(type)),"Current component survives .circ save/reopen: " + id);
      }
      require(loaded.getCurrentCircuit().getNonWires().size()==project.getCurrentCircuit().getNonWires().size(),"Save/reopen preserves all circuit components");
      System.out.println("PASS: " + checks + " current-upstream integration checks");
    } finally {
      project.getSimulator().shutDown();project.getLogisimFile().stopAutosaveThread(false);
      if(loaded!=null){loaded.getSimulator().shutDown();loaded.getLogisimFile().stopAutosaveThread(false);}
    }
    System.exit(0);
  }
}
