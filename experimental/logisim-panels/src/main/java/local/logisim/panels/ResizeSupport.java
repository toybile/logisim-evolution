package local.logisim.panels;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Resize hit areas are inside the border, so the circuit remains clickable outside. */
final class ResizeSupport extends MouseAdapter {
  static final int N=1,S=2,W=4,E=8;
  final JComponent target;
  final ModernWorkspace owner;
  final java.util.function.Supplier<Dimension> minimum;
  final java.util.function.BooleanSupplier enabled;
  Point origin;
  Rectangle start;
  int direction;
  ResizeSupport(JComponent target,ModernWorkspace owner,
      java.util.function.Supplier<Dimension> minimum,java.util.function.BooleanSupplier enabled){
    this.target=target;this.owner=owner;this.minimum=minimum;this.enabled=enabled;
    target.addMouseListener(this);target.addMouseMotionListener(this);
  }
  int hit(Point p){
    int edge=0;
    if(p.y<7)edge|=N;else if(p.y>=target.getHeight()-7)edge|=S;
    if(p.x<7)edge|=W;else if(p.x>=target.getWidth()-7)edge|=E;
    return edge;
  }
  static int cursor(int edge){
    return switch(edge){case N -> Cursor.N_RESIZE_CURSOR;case S -> Cursor.S_RESIZE_CURSOR;
      case W -> Cursor.W_RESIZE_CURSOR;case E -> Cursor.E_RESIZE_CURSOR;
      case N|W -> Cursor.NW_RESIZE_CURSOR;case N|E -> Cursor.NE_RESIZE_CURSOR;
      case S|W -> Cursor.SW_RESIZE_CURSOR;case S|E -> Cursor.SE_RESIZE_CURSOR;
      default -> Cursor.DEFAULT_CURSOR;};
  }
  public void mouseMoved(MouseEvent e){target.setCursor(Cursor.getPredefinedCursor(enabled.getAsBoolean()?cursor(hit(e.getPoint())):Cursor.DEFAULT_CURSOR));}
  public void mouseExited(MouseEvent e){if(origin==null)target.setCursor(Cursor.getDefaultCursor());}
  public void mousePressed(MouseEvent e){
    if(enabled.getAsBoolean()&&SwingUtilities.isLeftMouseButton(e))begin(e,hit(e.getPoint()));
  }
  void begin(MouseEvent e,int edge){
    if(edge==0)return;
    owner.surface.moveToFront(target);direction=edge;origin=e.getLocationOnScreen();start=target.getBounds();
  }
  public void mouseDragged(MouseEvent e){
    if(origin==null)return;
    Point p=e.getLocationOnScreen();
    Rectangle bounds=resizeBounds(start,direction,p.x-origin.x,p.y-origin.y,minimum.get(),owner.surface.getSize());
    target.setBounds(bounds);
    if(target instanceof FloatingToolbar rail)rail.ensureCapacity();
    target.revalidate();target.repaint();owner.scheduleSave();
  }
  public void mouseReleased(MouseEvent e){origin=null;target.setCursor(Cursor.getDefaultCursor());}
  void attachCorner(JComponent grip){
    grip.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
    grip.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent e){if(enabled.getAsBoolean()&&SwingUtilities.isLeftMouseButton(e))begin(e,S|E);}
      public void mouseReleased(MouseEvent e){ResizeSupport.this.mouseReleased(e);}
    });
    grip.addMouseMotionListener(new MouseMotionAdapter(){public void mouseDragged(MouseEvent e){ResizeSupport.this.mouseDragged(e);}});
  }
  static Rectangle resizeBounds(Rectangle r,int edge,int dx,int dy,Dimension min,Dimension surface){
    int left=r.x,right=r.x+r.width,top=r.y,bottom=r.y+r.height;
    int mw=Math.min(min.width,Math.max(1,surface.width-16));
    int mh=Math.min(min.height,Math.max(1,surface.height-16));
    if((edge&W)!=0)left=Math.max(8,Math.min(right-mw,left+dx));
    if((edge&E)!=0)right=Math.min(surface.width-8,Math.max(left+mw,right+dx));
    if((edge&N)!=0)top=Math.max(8,Math.min(bottom-mh,top+dy));
    if((edge&S)!=0)bottom=Math.min(surface.height-8,Math.max(top+mh,bottom+dy));
    return new Rectangle(left,top,right-left,bottom-top);
  }
}
