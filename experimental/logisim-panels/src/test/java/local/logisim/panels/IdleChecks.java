package local.logisim.panels;
import java.awt.*;
import java.nio.file.*;
import javax.swing.*;
import com.cburch.logisim.gui.main.Frame;
import com.formdev.flatlaf.FlatLightLaf;

/** Counts real repaint requests from unchanged simulator values; same harness runs on both versions. */
public final class IdleChecks {
  static class Counter extends RepaintManager {
    long requests;
    public synchronized void addDirtyRegion(JComponent c,int x,int y,int w,int h){requests++;super.addDirtyRegion(c,x,y,w,h);}
  }
  public static void main(String[] args)throws Exception{
    FlatLightLaf.setup();var project=Checks.demo();
    SwingUtilities.invokeAndWait(()->{
      try{
        Frame frame=new Frame(project);ModernWorkspace ui=ModernWorkspace.install(frame,Paths.get(args[0],"idle-layout.properties"));
        ui.statusTimer.stop();ui.simulation.refreshValues();
        Counter counter=new Counter();RepaintManager.setCurrentManager(counter);
        for(int i=0;i<1000;i++)ui.simulation.refreshValues();
        System.out.println("UNCHANGED_SIMULATION_REPAINTS="+counter.requests);
        ui.dispose();frame.dispose();
      }catch(Exception e){throw new RuntimeException(e);}
    });
    project.getSimulator().shutDown();project.getLogisimFile().stopAutosaveThread(false);System.exit(0);
  }
}
