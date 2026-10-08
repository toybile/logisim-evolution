package local.logisim.panels;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Larger outer curvature for zoom controls, with the normal control hit areas preserved. */
final class RoundedOverlay extends JPanel {
  static final int ARC=26;
  RoundedOverlay(){super(new FlowLayout(FlowLayout.CENTER,5,7));setOpaque(false);setBorder(new EmptyBorder(2,9,2,9));}
  protected void paintComponent(Graphics g){
    Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    p.setColor(Color.WHITE);p.fillRoundRect(0,0,getWidth()-1,getHeight()-1,ARC,ARC);
    p.setColor(new Color(216,225,237));p.drawRoundRect(0,0,getWidth()-1,getHeight()-1,ARC,ARC);p.dispose();
  }
  protected void paintChildren(Graphics g){
    Graphics2D p=(Graphics2D)g.create();p.clip(new RoundRectangle2D.Double(1,1,getWidth()-2,getHeight()-2,ARC-2,ARC-2));
    super.paintChildren(p);p.dispose();
  }
}
