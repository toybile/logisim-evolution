package local.logisim.panels;
import com.cburch.logisim.gui.main.Frame;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import javax.swing.*;
import javax.imageio.ImageIO;
public final class CanvasNavigationChecks {
  static ModernWorkspace ui;static Frame frame;static int checks;
  static void require(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);System.out.println("OK "+text);}
  public static void main(String[] args)throws Exception{
    Thread.setDefaultUncaughtExceptionHandler((thread,error) -> {error.printStackTrace();System.exit(1);});
    FlatLightLaf.setup();var project=Checks.demo();var original=project.getCurrentCircuit().getBounds();
    SwingUtilities.invokeAndWait(() -> {try{
      frame=new Frame(project);frame.setSize(1366,850);
      ui=ModernWorkspace.install(frame,Path.of(args[0],"layout.properties"));frame.setVisible(true);
    }catch(Exception e){throw new RuntimeException(e);}});
    SwingUtilities.invokeAndWait(() -> {
      var canvas=frame.getCanvas();double zoom=canvas.getCanvasPane().getZoomFactor();ui.centerCircuit();
      var bounds=project.getCurrentCircuit().getBounds();var view=canvas.getViewableRect();
      var point=canvas.circuitToCanvas(bounds.getX()+bounds.getWidth()/2.0,bounds.getY()+bounds.getHeight()/2.0);
      var physical=canvas.getCanvasPane().getViewport().getViewRect();
      require(Math.abs(point.x-physical.getCenterX())<=2,"circuit centered horizontally");
      require(Math.abs(point.y-physical.getCenterY())<=2,"circuit centered vertically");
      require(canvas.getCanvasPane().getZoomFactor()==zoom,"initial zoom preserved");
      require(bounds.equals(original),"saved component bounds unchanged");
      final Point[] received={null};var listener=new MouseAdapter(){public void mousePressed(MouseEvent e){received[0]=e.getPoint();}};
      canvas.addMouseListener(listener);
      for(double factor:new double[]{1.0,.5,1.5}){
        frame.getZoomModel().setZoomFactor(factor);ui.centerCircuit();
        var component=project.getCurrentCircuit().getNonWires().iterator().next();var location=component.getLocation();
        var click=canvas.circuitToCanvas(location.getX(),location.getY());
        canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,click.x,click.y,1,false,MouseEvent.BUTTON1));
        canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,click.x,click.y,1,false,MouseEvent.BUTTON1));
        require(received[0].x==location.getX()&&received[0].y==location.getY(),"mouse coordinates agree at zoom "+factor);
      }
      canvas.removeMouseListener(listener);
      frame.getZoomModel().setZoomFactor(.5);ui.centerCircuit();
      var before=canvas.getPreferredSize();canvas.getCanvasPane().getHorizontalScrollBar().setValue(0);
      var anchor=canvas.getViewableRect();ui.navigation.expand();var after=canvas.getViewableRect();
      require(canvas.getPreferredSize().width>before.width,"left edge grows workspace");
      require(Math.abs(anchor.x-after.x)<=2,"expansion preserves camera coordinates");
      for(int i=0;i<80;i++){canvas.getCanvasPane().getHorizontalScrollBar().setValue(0);ui.navigation.expand();}
      require(canvas.getPreferredSize().width<=Math.max(before.width,original.getWidth()*20*.5)+1,"workspace respects circuit-derived width cap");
      frame.getZoomModel().setZoomFactor(1);ui.centerCircuit();
    });
    SwingUtilities.invokeAndWait(() -> {try{
      var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
      var g=image.createGraphics();frame.paint(g);g.dispose();ImageIO.write(image,"png",Path.of(args[0],"centered-example.png").toFile());
      ui.restoreOriginal();require(frame.getCanvas().getDrawingOrigin().equals(new Point()),"legacy editor restores native coordinates");frame.dispose();
    }catch(Exception e){throw new RuntimeException(e);}});
    project.getSimulator().shutDown();project.getLogisimFile().stopAutosaveThread(false);
    System.out.println("PASS: "+checks+" canvas navigation checks");System.exit(0);
  }
}
