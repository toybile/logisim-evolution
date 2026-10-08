package local.logisim.panels;

import com.cburch.draw.toolbar.*;
import com.cburch.logisim.tools.Tool;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/** One responsive symbol toolbar; editing and panel visibility are independent states. */
final class FloatingToolbar extends JPanel {
  final ModernWorkspace owner;
  final List<AbstractButton> buttons=new ArrayList<>();
  final Map<String,JToggleButton> editButtons=new LinkedHashMap<>();
  final JLabel grip=new JLabel(new UiIcon("grip",18));
  final JLabel resizeGrip=new JLabel(new UiIcon("resize",15));
  final Toolbar nativeToolbar;
  ToolbarModel observed;
  int columns;
  final ToolbarModelListener modelListener=new ToolbarModelListener(){
    public void toolbarAppearanceChanged(ToolbarModelEvent e){SwingUtilities.invokeLater(FloatingToolbar.this::refreshSelection);}
    public void toolbarContentsChanged(ToolbarModelEvent e){SwingUtilities.invokeLater(FloatingToolbar.this::refreshSelection);}
  };
  FloatingToolbar(ModernWorkspace owner,Toolbar nativeToolbar){
    super(null);this.owner=owner;this.nativeToolbar=nativeToolbar;setOpaque(false);
    setBorder(new EmptyBorder(7,7,7,7));
    String[] names={"Edit Tool","Poke Tool","Wiring Tool","Text Tool"};
    String[] icons={"select","hand","wire","text"};
    String[] tips={I18n.t("Selecionar e mover componentes"),I18n.t("Interagir com o circuito"),I18n.t("Conectar fios"),I18n.t("Inserir texto")};
    for(int i=0;i<names.length;i++){
      final String name=names[i];JToggleButton b=toggle(icons[i],tips[i]);editButtons.put(name,b);
      b.addActionListener(e -> {
        Tool tool=ModernWorkspace.findTool(owner.project.getLogisimFile(),name,new HashSet<>());
        if(tool!=null){owner.project.setTool(tool.cloneTool());owner.frame.getCanvas().requestFocusInWindow();}
        refreshSelection();
      });addButton(b);
    }
    JButton more=iconButton("tools",I18n.t("Todas as ferramentas e atalhos configurados"));
    more.addActionListener(e -> showNativeTools(more));addButton(more);
    String[] ids={"library","properties","simulation","truth"};
    String[] labels={I18n.t("Biblioteca"),I18n.t("Propriedades"),I18n.t("Simulação"),I18n.t("Tabela verdade")};
    String[] panelIcons={"library","sliders","wave","table"};
    for(int i=0;i<ids.length;i++){
      final String id=ids[i];JToggleButton b=toggle(panelIcons[i],I18n.t("Abrir / fechar ")+labels[i]+" • Ctrl+Alt+"+(i+1));
      b.addActionListener(e -> {if(b.isSelected())owner.show(id);else owner.hide(id);});
      owner.toggles.put(id,b);addButton(b);
    }
    addAction("play",I18n.t("Executar simulação"),() -> owner.project.getSimulator().setAutoPropagation(true));
    addAction("pause",I18n.t("Pausar simulação"),() -> {owner.project.getSimulator().setAutoTicking(false);owner.project.getSimulator().setAutoPropagation(false);});
    addAction("step",I18n.t("Propagar uma etapa"),() -> owner.project.getSimulator().step());
    addAction("reset",I18n.t("Reiniciar simulação"),() -> owner.project.getSimulator().reset());
    JButton arrangement=iconButton("layout",I18n.t("Disposição dos painéis e da barra"));
    arrangement.addActionListener(e -> owner.showArrangement(arrangement));addButton(arrangement);
    addAction("help",I18n.t("Como usar a área de trabalho"),owner::showHelp);
    grip.setToolTipText(I18n.t("Arraste para mover a barra; redimensione pelas bordas ou pelo canto."));
    grip.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));add(grip);add(resizeGrip);
    ResizeSupport resize=new ResizeSupport(this,owner,() -> new Dimension(66,66),() -> true);resize.attachCorner(resizeGrip);
    MouseAdapter move=new MouseAdapter(){Point origin,start;
      public void mousePressed(MouseEvent e){if(!SwingUtilities.isLeftMouseButton(e))return;origin=e.getLocationOnScreen();start=getLocation();owner.surface.moveToFront(FloatingToolbar.this);}
      public void mouseDragged(MouseEvent e){if(origin==null)return;Point p=e.getLocationOnScreen();setLocation(start.x+p.x-origin.x,start.y+p.y-origin.y);owner.constrainToolbar();owner.scheduleSave();}
      public void mouseReleased(MouseEvent e){origin=null;}
    };grip.addMouseListener(move);grip.addMouseMotionListener(move);
    refreshSelection();
  }
  void addAction(String icon,String tip,Runnable action){JButton b=iconButton(icon,tip);b.addActionListener(e -> action.run());addButton(b);}
  void addButton(AbstractButton b){buttons.add(b);add(b);}
  static void style(AbstractButton b,String tip){
    I18n.tip(b,tip);b.getAccessibleContext().setAccessibleName(tip);b.setMargin(new Insets(0,0,0,0));
    b.putClientProperty("JButton.buttonType","borderless");b.putClientProperty("JToggleButton.buttonType","borderless");
    b.putClientProperty("JToggleButton.selectedBackground",new Color(226,240,255));
    b.putClientProperty("JToggleButton.selectedForeground",new Color(0,111,245));
    b.setForeground(new Color(23,36,55));b.setBackground(Color.WHITE);
  }
  static void paintSymbol(AbstractButton b,Graphics graphics){
    Graphics2D p=(Graphics2D)graphics.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    boolean selected=b instanceof JToggleButton&&b.isSelected();
    if(selected||b.getModel().isRollover()||b.getModel().isPressed()){
      p.setColor(selected?Appearance.selectionBackground():new Color(242,246,251));p.fillRoundRect(0,0,b.getWidth(),b.getHeight(),9,9);
    }
    b.setForeground(!b.isEnabled()?new Color(155,164,178):selected?Appearance.accent():new Color(24,37,57));
    if(b.getIcon()!=null)b.getIcon().paintIcon(b,p,(b.getWidth()-b.getIcon().getIconWidth())/2,(b.getHeight()-b.getIcon().getIconHeight())/2);
    if(b.isFocusOwner()){p.setColor(Appearance.accent());p.drawRoundRect(1,1,b.getWidth()-3,b.getHeight()-3,9,9);}p.dispose();
  }
  static JButton iconButton(String kind,String tip){JButton b=new JButton(new UiIcon(kind,23)){
    protected void paintComponent(Graphics g){paintSymbol(this,g);}
  };style(b,tip);return b;}
  static JToggleButton toggle(String kind,String tip){JToggleButton b=new JToggleButton(new UiIcon(kind,23)){
    protected void paintComponent(Graphics g){paintSymbol(this,g);}
  };style(b,tip);return b;}
  int columnCount(){return Math.min(buttons.size(),Math.max(1,(getWidth()-30)/44));}
  void ensureCapacity(){
    int count=buttons.size();if(count==0)return;
    int cols=columnCount(),rows=(count+cols-1)/cols;
    int maxHeight=Math.max(66,owner.surface.getHeight()-16);
    if(rows*44+28>maxHeight){
      int allowedRows=Math.max(1,(maxHeight-28)/44);cols=(count+allowedRows-1)/allowedRows;
      setSize(Math.max(getWidth(),cols*44+30),Math.min(getHeight(),maxHeight));
    }
    rows=(count+columnCount()-1)/columnCount();
    setSize(getWidth(),Math.max(Math.min(maxHeight,rows*44+28),Math.min(maxHeight,getHeight())));
    owner.constrainToolbar();doLayout();
  }
  public void doLayout(){
    if(buttons.isEmpty())return;
    columns=columnCount();int rows=(buttons.size()+columns-1)/columns;
    if(columns==1){
      grip.setBounds(22,6,18,18);
      for(int i=0;i<buttons.size();i++)buttons.get(i).setBounds(12,27+i*44,40,40);
    }else {
      grip.setBounds(7,Math.max(7,(getHeight()-18)/2),18,18);
      int y=Math.max(8,(getHeight()-rows*44)/2);
      for(int i=0;i<buttons.size();i++)buttons.get(i).setBounds(27+(i%columns)*44,y+(i/columns)*44,40,40);
    }
    resizeGrip.setBounds(getWidth()-22,getHeight()-22,15,15);
  }
  protected void paintComponent(Graphics g){
    Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    p.setColor(new Color(229,233,240));p.fillRoundRect(2,3,getWidth()-3,getHeight()-4,18,18);
    p.setColor(Color.WHITE);p.fillRoundRect(0,0,getWidth()-3,getHeight()-4,18,18);
    p.setColor(new Color(210,219,232));p.drawRoundRect(0,0,getWidth()-3,getHeight()-4,18,18);
    p.setColor(new Color(218,225,235));
    if(columns==1){for(int n:new int[]{5,9,13})if(n<buttons.size()){int y=buttons.get(n).getY()-2;p.drawLine(14,y,getWidth()-14,y);}}
    else if(columns==buttons.size()){for(int n:new int[]{5,9,13}){int x=buttons.get(n).getX()-3;p.drawLine(x,13,x,getHeight()-13);}}
    p.dispose();
  }
  void refreshLanguage(){
    String[] ids={"library","properties","simulation","truth"};
    String[] labels={"Biblioteca","Propriedades","Simulação","Tabela verdade"};
    for(int i=0;i<ids.length;i++){
      JToggleButton b=owner.toggles.get(ids[i]);String tip=I18n.t("Abrir / fechar ")+I18n.t(labels[i])+" • Ctrl+Alt+"+(i+1);
      b.setToolTipText(tip);b.getAccessibleContext().setAccessibleName(tip);
    }
  }
  void refreshSelection(){
    ToolbarModel model=nativeToolbar.getToolbarModel();
    if(model!=observed){if(observed!=null)observed.removeToolbarModelListener(modelListener);observed=model;if(model!=null)model.addToolbarModelListener(modelListener);}
    Tool selected=owner.project.getTool();
    editButtons.forEach((name,b) -> b.setSelected(selected!=null&&name.equals(selected.getName())));
    owner.toggles.forEach((id,b) -> {FloatingPanel panel=owner.panels.get(id);if(panel!=null)b.setSelected(panel.isOpen());});
    boolean editing=com.cburch.logisim.gui.main.Frame.EDIT_LAYOUT.equals(owner.frame.getEditorView());
    editButtons.values().forEach(b -> b.setEnabled(editing));
  }
  JPopupMenu nativeToolsMenu(){
    JPopupMenu menu=new JPopupMenu();ToolbarModel model=nativeToolbar.getToolbarModel();
    if(model!=null)for(ToolbarItem item:model.getItems()){
      if(item.getToolTip()==null || item.getToolTip().isBlank()){menu.addSeparator();continue;}
      JCheckBoxMenuItem entry=new JCheckBoxMenuItem(item.getToolTip(),model.isSelected(item));
      entry.setEnabled(item.isSelectable());entry.addActionListener(e -> {model.itemSelected(item);refreshSelection();});menu.add(entry);
    }
    return menu;
  }
  void showNativeTools(JComponent anchor){nativeToolsMenu().show(anchor,0,anchor.getHeight());}
  void dispose(){if(observed!=null)observed.removeToolbarModelListener(modelListener);}
}
