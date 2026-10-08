package local.logisim.panels;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/** A modeless overlay: its transparent surroundings never intercept the canvas. */
final class FloatingPanel extends JPanel {
  final String id;
  final JPanel body = new JPanel(new BorderLayout());
  final JPanel header = new JPanel(new BorderLayout(6, 0));
  final ModernWorkspace owner;
  boolean minimized;
  int expandedHeight;
  final Dimension minimum;
  private final JButton minimize;
  private boolean open=true;
  private float visibilityAlpha=1f;
  private javax.swing.Timer transition;
  private java.awt.image.BufferedImage paintBuffer;

  FloatingPanel(ModernWorkspace owner, String id, String title, JComponent content,
      Dimension minimum) {
    super(new BorderLayout());
    this.owner = owner; this.id = id; this.minimum = minimum;
    setOpaque(false); setBackground(Color.WHITE);
    setBorder(new EmptyBorder(7,7,7,7));
    header.setBackground(Appearance.mix(Color.WHITE,Appearance.canvas(),.85f));
    header.setBorder(new EmptyBorder(3,10,3,4));
    header.setPreferredSize(new Dimension(100, 38));
    JLabel caption = I18n.label(title,new UiIcon("grip"),SwingConstants.LEFT);
    caption.setFont(UiFonts.font("Segoe UI", Font.BOLD, 13));
    I18n.tip(caption,I18n.t("Arraste o título para mover. Clique duas vezes para minimizar."));
    header.add(caption, BorderLayout.CENTER);
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
    controls.setOpaque(false);
    minimize = small("−", I18n.t("Minimizar / expandir"), () -> setMinimized(!minimized));
    controls.add(minimize);
    controls.add(small("×", I18n.t("Fechar painel (reabra pelo símbolo na barra)"), () -> owner.hide(id)));
    header.add(controls, BorderLayout.EAST);
    add(header, BorderLayout.NORTH);
    body.setBackground(Color.WHITE); body.setBorder(new EmptyBorder(8,9,8,9));
    body.add(content, BorderLayout.CENTER); add(body, BorderLayout.CENTER);
    JPanel footer = new JPanel(new BorderLayout());
    footer.setOpaque(false); footer.setPreferredSize(new Dimension(100, 12));
    JLabel grip = new JLabel(new UiIcon("resize"));
    grip.setForeground(new Color(147,157,173));
    I18n.tip(grip,I18n.t("Arraste para redimensionar"));
    grip.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
    footer.add(grip, BorderLayout.EAST); add(footer, BorderLayout.SOUTH);
    MouseAdapter drag = new MouseAdapter() {
      Point origin, start;
      public void mousePressed(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        owner.bringToFront(FloatingPanel.this);
        origin = e.getLocationOnScreen(); start = getLocation();
      }
      public void mouseDragged(MouseEvent e) {
        if (origin == null) return;
        Point p=e.getLocationOnScreen();
        setLocation(start.x+p.x-origin.x,start.y+p.y-origin.y);
        owner.constrain(FloatingPanel.this); owner.scheduleSave();
      }
      public void mouseReleased(MouseEvent e) { origin=null; }
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount()==2 && SwingUtilities.isLeftMouseButton(e))
          setMinimized(!minimized);
      }
    };
    header.addMouseListener(drag); header.addMouseMotionListener(drag);
    caption.addMouseListener(drag); caption.addMouseMotionListener(drag);
    ResizeSupport resize=new ResizeSupport(this,owner,() -> minimum,() -> !minimized);
    resize.attachCorner(grip);
    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) { owner.bringToFront(FloatingPanel.this); }
    });
    InputMap keys=getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    keys.put(KeyStroke.getKeyStroke("ESCAPE"),"hide");
    getActionMap().put("hide",new AbstractAction() {
      public void actionPerformed(ActionEvent e) { owner.hide(id); }
    });
  }
  boolean isOpen(){return open;}
  public void setVisible(boolean visible){
    if(transition!=null){transition.stop();transition=null;}
    open=visible;visibilityAlpha=visible?1f:0f;super.setVisible(visible);if(!visible)paintBuffer=null;
  }
  void animateVisible(boolean visible){
    if(!Appearance.animations||!owner.frame.isShowing()){setVisible(visible);return;}
    if(open==visible)return;
    open=visible;
    if(transition!=null)transition.stop();
    if(visible&&!super.isVisible()){visibilityAlpha=0f;super.setVisible(true);}
    final float start=visibilityAlpha,target=visible?1f:0f;
    final long begun=System.nanoTime();
    transition=new javax.swing.Timer(16,e -> {
      float progress=Math.min(1f,(System.nanoTime()-begun)/160_000_000f);
      float eased=progress*progress*(3-2*progress);
      visibilityAlpha=start+(target-start)*eased;repaint();
      if(progress>=1f){((javax.swing.Timer)e.getSource()).stop();transition=null;
        if(!open){super.setVisible(false);paintBuffer=null;}owner.surface.repaint(getBounds());}
    });transition.start();repaint();
  }
  public boolean contains(int x,int y){return open&&super.contains(x,y);}
  protected boolean isPaintingOrigin(){return true;}
  public void paint(Graphics graphics){
    if(getWidth()<=0||getHeight()<=0)return;
    Graphics2D screen=(Graphics2D)graphics.create();
    if(Appearance.opacity==1f&&visibilityAlpha==1f){super.paint(screen);screen.dispose();return;}
    double scaleX=Math.abs(screen.getTransform().getScaleX()),scaleY=Math.abs(screen.getTransform().getScaleY());
    int width=Math.max(1,(int)Math.ceil(getWidth()*scaleX)),height=Math.max(1,(int)Math.ceil(getHeight()*scaleY));
    if(paintBuffer==null||paintBuffer.getWidth()!=width||paintBuffer.getHeight()!=height)
      paintBuffer=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D buffer=paintBuffer.createGraphics();buffer.setComposite(AlphaComposite.Clear);
    buffer.fillRect(0,0,width,height);buffer.setComposite(AlphaComposite.SrcOver);buffer.scale(scaleX,scaleY);
    super.paint(buffer);buffer.dispose();
    screen.setComposite(AlphaComposite.SrcOver.derive(Appearance.opacity*visibilityAlpha));
    screen.drawImage(paintBuffer,0,0,getWidth(),getHeight(),null);screen.dispose();
  }
  void dispose(){if(transition!=null)transition.stop();paintBuffer=null;}
  protected void paintComponent(Graphics g){
    Graphics2D p=(Graphics2D)g.create();
    p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    p.setColor(Color.WHITE);p.fillRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);
    p.setColor(new Color(211,220,232));p.drawRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);
    p.dispose();
  }
  protected void paintChildren(Graphics g){
    Graphics2D p=(Graphics2D)g.create();
    p.clip(new java.awt.geom.RoundRectangle2D.Double(1,1,getWidth()-2,getHeight()-2,17,17));
    super.paintChildren(p);p.dispose();
  }
  private JButton small(String label, String tip, Runnable action) {
    JButton b=new JButton(label); b.setFocusable(true); I18n.tip(b,tip);
    b.getAccessibleContext().setAccessibleName(tip);
    b.putClientProperty("JButton.buttonType","borderless");
    b.setPreferredSize(new Dimension(27,27)); b.setMargin(new Insets(0,0,0,0));
    b.addActionListener(e -> action.run()); return b;
  }
  void setMinimized(boolean value) {
    if(value==minimized)return;
    if(value){expandedHeight=getHeight();setSize(getWidth(),52);}
    else setSize(getWidth(),Math.max(minimum.height,expandedHeight));
    minimized=value; body.setVisible(!value); minimize.setText(value?"□":"−");
    owner.constrain(this); revalidate(); owner.scheduleSave();
  }
}
