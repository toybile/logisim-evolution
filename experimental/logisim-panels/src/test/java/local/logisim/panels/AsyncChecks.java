package local.logisim.panels;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.formdev.flatlaf.FlatLightLaf;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

public final class AsyncChecks {
  static ModernWorkspace ui;
  static Frame frame;
  static Project project;
  static int checks;
  static void require(boolean condition,String message){
    checks++;if(!condition)throw new AssertionError(message);System.out.println("OK "+message);
  }
  static void waitForTable() throws Exception {
    long deadline=System.nanoTime()+5_000_000_000L;
    AtomicBoolean ready=new AtomicBoolean();
    while(System.nanoTime()<deadline){
      SwingUtilities.invokeAndWait(() -> ready.set(ui.truth.table.getRowCount()==8));
      if(ready.get())return;Thread.sleep(25);
    }
    throw new AssertionError("Tabela não concluiu em cinco segundos");
  }
  static List<JToggleButton> toggles(java.awt.Container container){
    List<JToggleButton> found=new ArrayList<>();
    for(var component:container.getComponents()){
      if(component instanceof JToggleButton toggle && !(toggle instanceof JCheckBox))found.add(toggle);
      if(component instanceof java.awt.Container nested)found.addAll(toggles(nested));
    }
    return found;
  }
  public static void main(String[] args) throws Exception {
    FlatLightLaf.setup();project=Checks.demo();
    Path root=Paths.get(args.length>0?args[0]:".").toAbsolutePath();
    SwingUtilities.invokeAndWait(() -> {
      try{
        frame=new Frame(project);
        ui=ModernWorkspace.install(frame,root.resolve("check-async-disposicao.properties"));
        ui.surface.setSize(1200,700);ui.arrangeSurface();
        ui.show("truth");
      }catch(Exception e){throw new RuntimeException(e);}
    });
    waitForTable();
    SwingUtilities.invokeAndWait(() -> {
      require(ui.truth.table.getRowCount()==8,"abrir painel inicia análise em segundo plano");
      require(ui.truth.table.getValueAt(5,3).equals("0"),"análise da cópia usa valores reais do circuito");
      ui.show("simulation");
      var buttons=toggles(ui.simulation);
      require(buttons.size()==3,"painel cria controles para as três entradas");
      buttons.get(0).doClick();buttons.get(2).doClick();
      var pins=Analyze.getPinLabels(project.getCurrentCircuit());
      for(var entry:pins.entrySet()){
        if(!Pin.FACTORY.isInputPin(entry.getKey()))continue;
        Value value=Pin.FACTORY.getValue(project.getCircuitState().getInstanceState(entry.getKey()));
        boolean expected=!entry.getValue().equals("B");
        require(Value.TRUE.equals(value)==expected,"controle "+entry.getValue()+" altera o pino real");
      }
      project.getSimulator().setAutoPropagation(true);
    });
    long deadline=System.nanoTime()+2_000_000_000L;
    Instance output=null;
    for(var entry:Analyze.getPinLabels(project.getCurrentCircuit()).entrySet())
      if(!Pin.FACTORY.isInputPin(entry.getKey()))output=entry.getKey();
    while(System.nanoTime()<deadline &&
        !Value.FALSE.equals(Pin.FACTORY.getValue(project.getCircuitState().getInstanceState(output))))Thread.sleep(20);
    require(Value.FALSE.equals(Pin.FACTORY.getValue(project.getCircuitState().getInstanceState(output))),
        "alterações pelo painel chegam à saída do simulador");
    SwingUtilities.invokeAndWait(() -> {
      ui.truth.invalidateResult();ui.truth.ensureCalculated();ui.truth.invalidateResult();ui.truth.ensureCalculated();
    });
    waitForTable();
    SwingUtilities.invokeAndWait(() -> {
      require(ui.truth.table.getRowCount()==8,"recalcular e invalidar descarta respostas antigas");
      ui.dispose();frame.dispose();
    });
    project.getSimulator().shutDown();project.getLogisimFile().stopAutosaveThread(false);
    System.out.println("PASSOU: "+checks+" verificações complementares");
    System.exit(0);
  }
}
