package local.logisim.panels;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;

/** Change only presentation colors in this process, never persisted simulator preferences. */
final class CanvasSkin {
  final Canvas canvas;
  final Color oldFalse=Value.falseColor,oldTrue=Value.trueColor;
  final Color oldBackground;
  Object grid;
  Field imageField;
  Image original,last;
  Color lastTint;
  Color lastAccent;
  CanvasSkin(Canvas canvas){
    this.canvas=canvas;
    oldBackground=canvas.getBackground();canvas.setBackground(Appearance.canvas());
    com.cburch.logisim.std.wiring.Probe.setModernPinValues(true);
    Value.falseColor=new Color(122,132,149);Value.trueColor=Appearance.signalOne();
    try{
      Method getter=Canvas.class.getDeclaredMethod("getGridPainter");getter.setAccessible(true);grid=getter.invoke(canvas);
      imageField=grid.getClass().getDeclaredField("gridImage");imageField.setAccessible(true);
    }catch(ReflectiveOperationException failure){failure.printStackTrace();}
  }
  void refresh(){
    if(!canvas.getBackground().equals(Appearance.canvas()))canvas.setBackground(Appearance.canvas());
    if(grid==null)return;
    try{
      Image source=(Image)imageField.get(grid);if(source==null)return;
      Color background=Appearance.canvas();
      if(source==last){if(background.equals(lastTint)&&Appearance.accent().equals(lastAccent))return;source=original;}
      int w=source.getWidth(canvas),h=source.getHeight(canvas);if(w<=0||h<=0)return;
      BufferedImage tint=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics=tint.createGraphics();graphics.drawImage(source,0,0,canvas);graphics.dispose();
      for(int y=0;y<h;y++)for(int x=0;x<w;x++){
        int rgb=tint.getRGB(x,y),luma=((rgb>>16&255)+(rgb>>8&255)+(rgb&255))/3;
        tint.setRGB(x,y,(luma>200?background:Appearance.mix(background,Appearance.accent(),.23f)).getRGB());
      }
      original=source;last=tint;lastTint=background;lastAccent=Appearance.accent();imageField.set(grid,tint);canvas.repaint();
    }catch(ReflectiveOperationException failure){failure.printStackTrace();grid=null;}
  }
  void restore(){
    canvas.setBackground(oldBackground);
    com.cburch.logisim.std.wiring.Probe.setModernPinValues(false);
    Value.falseColor=oldFalse;Value.trueColor=oldTrue;
    if(grid!=null&&original!=null)try{imageField.set(grid,original);}catch(ReflectiveOperationException ignored){}
  }
}
