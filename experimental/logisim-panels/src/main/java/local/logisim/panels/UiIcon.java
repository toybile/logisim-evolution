package local.logisim.panels;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;
final class UiIcon implements Icon {
  final String kind;final int size;
  UiIcon(String kind){this(kind,18);}
  UiIcon(String kind,int size){this.kind=kind;this.size=size;}
  public int getIconWidth(){return size;}public int getIconHeight(){return size;}
  static Path2D path(double... points){
    Path2D p=new Path2D.Double();p.moveTo(points[0],points[1]);
    for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);return p;
  }
  public void paintIcon(Component c,Graphics g,int x,int y){
    Graphics2D p=(Graphics2D)g.create();p.translate(x,y);p.scale(size/24.0,size/24.0);
    p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    p.setColor(c.getForeground());p.setStroke(new BasicStroke(size>=36?1.25f:1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    switch(kind){
      case "play":p.setColor(new Color(20,177,86));p.fill(path(6,3,6,21,21,12,6,3));break;
      case "pause":p.fillRoundRect(6,4,4,16,1,1);p.fillRoundRect(14,4,4,16,1,1);break;
      case "step":p.draw(path(4,4,4,20,16,12,4,4));p.drawLine(20,4,20,20);break;
      case "reset":p.drawArc(3,3,18,18,35,295);p.draw(path(16,3,21,3,21,8));break;
      case "grip":p.setColor(new Color(76,92,115));for(int r=0;r<3;r++)for(int col=0;col<2;col++)p.fillOval(7+col*7,4+r*6,3,3);break;
      case "chevron":p.draw(path(6,9,12,15,18,9));break;
      case "chevron-right":p.draw(path(9,6,15,12,9,18));break;
      case "resize":p.setColor(new Color(114,128,148));for(int i=0;i<3;i++)p.drawLine(9+i*4,21,21,9+i*4);break;
      case "select":p.fill(path(4,2,4,20,9,15,13,23,16,21,12,14,20,13,4,2));break;
      case "hand":p.draw(path(7,14,7,6,9,6,9,13,9,3,12,3,12,12,12,4,15,4,15,13,15,7,18,7,18,17,16,21,9,21,3,15,3,12,5,12,7,14));break;
      case "wire":p.drawLine(5,19,19,5);p.drawOval(2,16,6,6);p.drawOval(16,2,6,6);break;
      case "text":p.setFont(new Font("Serif",Font.BOLD,24));p.drawString("T",4,21);break;
      case "tools":p.drawRect(3,4,18,16);p.draw(path(8,12,12,8,16,12,12,16,8,12));break;
      case "sliders":for(int r=0;r<3;r++){int yy=5+r*7,xx=r==1?15:8;p.drawLine(2,yy,xx-3,yy);p.drawLine(xx+3,yy,22,yy);p.drawOval(xx-3,yy-3,6,6);}break;
      case "wave":Path2D wave=new Path2D.Double();wave.moveTo(2,17);wave.curveTo(9,20,5,3,11,4);wave.curveTo(17,5,11,23,18,19);wave.curveTo(21,17,21,11,23,11);p.draw(wave);break;
      case "table":p.drawRect(2,3,20,18);p.drawLine(2,9,22,9);p.drawLine(9,9,9,21);p.drawLine(16,9,16,21);p.drawLine(2,15,22,15);break;
      case "layout":p.drawRect(2,3,20,18);p.drawLine(2,8,22,8);p.drawRect(6,11,7,6);break;
      case "help":p.drawOval(2,2,20,20);p.setFont(UiFonts.font("Segoe UI",Font.BOLD,18));p.drawString("?",7,19);break;
      case "search":p.drawOval(3,3,12,12);p.drawLine(13,13,21,21);break;
      case "download":p.draw(path(12,2,12,16,6,10));p.drawLine(12,16,18,10);p.draw(path(3,16,3,21,21,21));break;
      case "input":p.drawRect(5,5,14,14);break;
      case "output":p.drawOval(4,4,16,16);break;
      case "not":p.draw(path(5,4,5,20,18,12,5,4));p.drawOval(18,10,4,4);p.drawLine(1,12,5,12);p.drawLine(22,12,24,12);break;
      case "and":case "library":Path2D and=new Path2D.Double();and.moveTo(6,4);and.lineTo(12,4);and.curveTo(23,4,23,20,12,20);and.lineTo(6,20);and.closePath();p.draw(and);p.drawLine(1,8,6,8);p.drawLine(1,16,6,16);p.drawLine(20,12,24,12);break;
      case "or":case "xor":Path2D or=new Path2D.Double();or.moveTo(6,4);or.curveTo(13,4,18,5,22,12);or.curveTo(18,19,13,20,6,20);or.curveTo(11,12,11,12,6,4);p.draw(or);p.drawLine(1,8,8,8);p.drawLine(1,16,8,16);p.drawLine(22,12,24,12);if(kind.equals("xor")){Path2D curve=new Path2D.Double();curve.moveTo(3,4);curve.curveTo(8,12,8,12,3,20);p.draw(curve);}break;
    }
    p.dispose();
  }
}
