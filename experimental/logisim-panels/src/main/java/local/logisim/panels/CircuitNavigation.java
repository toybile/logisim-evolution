package local.logisim.panels;

import com.cburch.logisim.gui.main.Canvas;
import java.awt.Rectangle;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

/** Expands presentation space without relocating circuit components. */
final class CircuitNavigation {
  private final Canvas canvas;
  private final WeakHashMap<com.cburch.logisim.circuit.Circuit,Rectangle> areas=new WeakHashMap<>();
  private final ChangeListener listener=e -> schedule();
  private boolean queued,changing,disposed;
  CircuitNavigation(Canvas canvas){
    this.canvas=canvas;
    canvas.getCanvasPane().getViewport().addChangeListener(listener);
  }
  private static int snap(double value){return (int)Math.floor(value/10)*10;}
  void prepare(){
    var circuit=canvas.getProject().getCurrentCircuit();if(circuit==null)return;
    var area=areas.get(circuit);
    if(area==null){
      var bounds=circuit.getBounds();var view=canvas.getCanvasPane().getViewport().getExtentSize();
      int width=Math.max(view.width,bounds.getX()+bounds.getWidth()+view.width);
      int height=Math.max(view.height,bounds.getY()+bounds.getHeight()+view.height);
      area=new Rectangle(snap(bounds.getX()+bounds.getWidth()/2.0-width/2.0),
          snap(bounds.getY()+bounds.getHeight()/2.0-height/2.0),width,height);
      areas.put(circuit,area);
    }
    changing=true;try{canvas.setWorkspaceArea(area);}finally{changing=false;}
  }
  private void schedule(){
    if(disposed||changing||queued||canvas.getCanvasPane().getZoomFactor()>=1)return;
    queued=true;SwingUtilities.invokeLater(() -> {queued=false;if(!disposed)expand();});
  }
  void expand(){
    var circuit=canvas.getProject().getCurrentCircuit();var area=areas.get(circuit);
    if(area==null||canvas.getCanvasPane().getZoomFactor()>=1)return;
    var bounds=circuit.getBounds();var view=canvas.getViewableRect();
    long capW=Math.max(area.width,20L*Math.max(1,bounds.getWidth()));
    long capH=Math.max(area.height,20L*Math.max(1,bounds.getHeight()));
    int stepX=Math.max(200,snap(view.width/2.0)),stepY=Math.max(200,snap(view.height/2.0));
    Rectangle next=new Rectangle(area);
    Rectangle visible=area.union(view);
    if(visible.width<=capW){next.x=visible.x;next.width=visible.width;}
    if(visible.height<=capH){next.y=visible.y;next.height=visible.height;}
    int growX=capW<view.width?0:(int)Math.min(stepX,capW-next.width);
    int growY=capH<view.height?0:(int)Math.min(stepY,capH-next.height);
    if(view.x-area.x<40){next.x-=growX;next.width+=growX;}
    else if(area.x+area.width-view.x-view.width<40)next.width+=growX;
    if(view.y-area.y<40){next.y-=growY;next.height+=growY;}
    else if(area.y+area.height-view.y-view.height<40)next.height+=growY;
    if(!next.equals(area)){areas.put(circuit,next);changing=true;try{canvas.setWorkspaceArea(next);}finally{changing=false;}}
  }
  void dispose(){disposed=true;canvas.getCanvasPane().getViewport().removeChangeListener(listener);canvas.setWorkspaceArea(null);}
}
