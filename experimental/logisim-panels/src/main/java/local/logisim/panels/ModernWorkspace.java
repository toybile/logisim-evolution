package local.logisim.panels;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.proj.*;
import com.cburch.logisim.tools.*;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

final class ModernWorkspace extends JPanel {
  final Frame frame;
  final Project project;
  final Path layoutFile;
  final JLayeredPane surface = new JLayeredPane();
  final Map<String,FloatingPanel> panels = new LinkedHashMap<>();
  final Map<String,JToggleButton> toggles = new LinkedHashMap<>();
  final List<Slot> slots = new ArrayList<>();
  final Container original;
  final JPanel nativeView;
  final JPanel tools = new JPanel(new BorderLayout());
  final JLabel projectName = new JLabel();
  final JLabel status = new JLabel();
  final JLabel feedback = I18n.label(I18n.t("Arraste os títulos dos painéis para organizar sua área de trabalho."));
  final JLabel propertiesHint=I18n.label(I18n.t("Selecione um componente na grade para editar."));
  final JLabel positionInfo=new JLabel();
  final AttrTable nativeAttributes;
  final PropertiesForm propertyForm;
  final JTabbedPane attributesTabs;
  final javax.swing.Timer saveTimer;
  final javax.swing.Timer statusTimer;
  final SimulationPanel simulation;
  final CanvasSkin canvasSkin;
  final TruthPanel truth;
  final FloatingToolbar toolbar;
  final LibraryPanel library;
  final JPanel zoomOverlay=new RoundedOverlay();
  final JMenuBar nativeMenu;
  final List<Component> menuDecorations=new ArrayList<>();
  private boolean initialized, retired;
  private Properties saved = new Properties();
  private final ProjectListener projectListener = this::onProjectEvent;
  private void onProjectEvent(ProjectEvent e) {
    if(e.getAction()==ProjectEvent.REPAINT_REQUEST)return;
    SwingUtilities.invokeLater(() -> {
      if(retired)return;
      updateHeader();
      library.nextStructureCheck=0;
      if(e.getAction()==ProjectEvent.ACTION_SET_CURRENT ||
          e.getAction()==ProjectEvent.ACTION_SET_FILE || e.getAction()==ProjectEvent.ACTION_COMPLETE ||
          e.getAction()==ProjectEvent.UNDO_COMPLETE || e.getAction()==ProjectEvent.REDO_COMPLETE)
        simulation.invalidatePins();
    });
  }

  static ModernWorkspace install(Frame frame, Path path) throws Exception {
    ModernWorkspace ui=new ModernWorkspace(frame,path);
    frame.setContentPane(ui);
    frame.getRootPane().putClientProperty("floating.workspace",ui);
    frame.revalidate(); frame.repaint();
    return ui;
  }
  ModernWorkspace(Frame frame,Path path) throws Exception {
    super(new BorderLayout());
    this.frame=frame; layoutFile=path; original=frame.getContentPane();
    project=(Project)field(frame,"project");
    // Resolve the complete integration surface before detaching any native control.
    nativeView=(JPanel)field(frame,"rightPanel");
    JComponent toolbox=(JComponent)field(frame,"toolbox");
    JTabbedPane attributes=(JTabbedPane)field(frame,"bottomTab");
    attributesTabs=attributes;
    nativeAttributes=(AttrTable)field(frame,"attrTable");
    Toolbar nativeToolbar=frame.getToolbar();
    nativeMenu=frame.getJMenuBar();
    JComponent simExplorer=(JComponent)field(frame,"simExplorer");
    try(InputStream in=Files.newInputStream(path)){saved.load(in);}
    catch(IOException ignored){}
    saveTimer=new javax.swing.Timer(500,e -> saveLayout()); saveTimer.setRepeats(false);
    canvasSkin=new CanvasSkin(frame.getCanvas());
    simulation=new SimulationPanel(project,() -> hint(I18n.t("Entrada alterada.")));
    truth=new TruthPanel(project,frame);

    detach(nativeView); detach(toolbox); detach(attributes); detach(nativeToolbar); detach(simExplorer);
    surface.setOpaque(false);
    surface.add(nativeView,JLayeredPane.DEFAULT_LAYER);
    surface.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) { arrangeSurface(); }
    });
    add(surface,BorderLayout.CENTER);
    toolbar=new FloatingToolbar(this,nativeToolbar);
    surface.add(toolbar,JLayeredPane.PALETTE_LAYER);
    decorateMenu();
    surface.add(buildStatus(),JLayeredPane.PALETTE_LAYER);
    library=new LibraryPanel(this,toolbox);
    addPanel("library",I18n.t("Biblioteca"),library,new Dimension(300,360));
    JPanel propertiesContent=new JPanel(new BorderLayout(0,6));
    propertiesHint.setFont(UiFonts.font("Segoe UI",Font.PLAIN,11));
    I18n.tip(propertiesHint,I18n.t("Selecione um componente na grade para editar suas propriedades."));
    propertiesHint.setForeground(new Color(107,119,136));
    positionInfo.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));
    positionInfo.setForeground(new Color(94,109,129));
    JPanel propertiesIntro=new JPanel(new BorderLayout(0,4));
    propertiesIntro.add(propertiesHint,BorderLayout.NORTH);
    propertiesIntro.add(positionInfo,BorderLayout.SOUTH);
    propertiesContent.add(propertiesIntro,BorderLayout.NORTH);
    propertiesContent.add(attributes,BorderLayout.CENTER);
    JComponent originalProperties=(JComponent)attributes.getComponentAt(0);
    attributes.setComponentAt(0,new JPanel());
    propertyForm=new PropertiesForm(this,nativeAttributes,originalProperties);
    attributes.setComponentAt(0,propertyForm);
    styleAttributes(nativeAttributes);
    addPanel("properties",I18n.t("Propriedades"),propertiesContent,new Dimension(290,180));
    JTabbedPane simulationTabs=new JTabbedPane();
    I18n.tab(simulationTabs,I18n.t("Sinais"),simulation);
    I18n.tab(simulationTabs,I18n.t("Estados internos"),simExplorer);
    addPanel("simulation",I18n.t("Simulação"),simulationTabs,new Dimension(410,240));
    addPanel("truth",I18n.t("Tabela verdade"),truth,new Dimension(370,220));
    polish(this);
    bindKeys();
    project.addProjectListener(projectListener);
    statusTimer=new javax.swing.Timer(250,e -> {
      if(frame.isShowing() && (frame.getExtendedState()&java.awt.Frame.ICONIFIED)==0 && !retired) {
        updateHeader();canvasSkin.refresh();toolbar.refreshSelection();library.refresh();if(panels.get("properties").isVisible())propertyForm.refresh(); if(panels.get("simulation").isVisible()) simulation.refreshValues();
        if(panels.get("truth").isVisible())truth.highlightCurrentInputs();
      }
    });
    statusTimer.start();
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosed(WindowEvent e) { dispose(); }
    });
    updateHeader();
    Appearance.workspaces.add(this);
    nativeMenu.setBackground(Appearance.mix(Color.WHITE,Appearance.canvas(),.7f));
  }
  static Object field(Object object,String name) throws Exception {
    Field field=Frame.class.getDeclaredField(name);field.setAccessible(true);return field.get(object);
  }
  private void detach(JComponent component) {
    slots.add(new Slot(component));
    if(component.getParent()!=null)component.getParent().remove(component);
  }
  private void decorateMenu(){
    JLabel brand=new JLabel("Logisim Evolution",new UiIcon("tools",23),SwingConstants.LEFT);
    brand.setFont(UiFonts.font("Segoe UI",Font.BOLD,15));brand.setBorder(new EmptyBorder(4,10,4,15));
    projectName.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));projectName.setBorder(new EmptyBorder(4,0,4,20));
    nativeMenu.add(brand,0);nativeMenu.add(projectName,1);menuDecorations.add(brand);menuDecorations.add(projectName);
    nativeMenu.setBorder(new MatteBorder(0,0,1,0,new Color(218,226,237)));
  }
  void showArrangement(JComponent anchor){
    JPopupMenu menu=new JPopupMenu();
    item(menu,I18n.t("Temas e aparência…"),() -> Appearance.show(this));menu.addSeparator();
    item(menu,I18n.t("Salvar disposição agora"),() -> {saveLayout();hint(I18n.t("Disposição salva."));});
    item(menu,I18n.t("Restaurar disposição inicial"),() -> {defaultLayout();hint(I18n.t("Disposição inicial restaurada."));});
    item(menu,I18n.t("Barra horizontal"),() -> {toolbar.setSize(690,72);toolbar.ensureCapacity();scheduleSave();});
    item(menu,I18n.t("Barra vertical"),() -> {toolbar.setSize(74,688);toolbar.ensureCapacity();scheduleSave();});
    item(menu,I18n.t("Ocultar todos os painéis · Ctrl+Alt+0"),() -> panels.keySet().forEach(this::hide));
    item(menu,I18n.t("Mostrar todos os painéis"),() -> panels.keySet().forEach(this::show));
    menu.addSeparator();item(menu,I18n.t("Voltar ao editor original"),this::restoreOriginal);menu.show(anchor,0,anchor.getHeight());
  }
  void showHelp(){
    JOptionPane.showMessageDialog(frame,
      I18n.t("Abra ou feche os painéis pelos símbolos da barra. A cor de seleção indica painel aberto.\n")
      +I18n.t("Arraste o cabeçalho para mover um painel. Redimensione por qualquer borda ou canto.\n")
      +I18n.t("O canto inferior direito ajusta altura e largura ao mesmo tempo.\n")
      +I18n.t("Arraste os pontos da barra para movê-la e redimensione para reorganizar os símbolos.\n")
      +I18n.t("Posição, tamanho e visibilidade ficam salvos.\n\n")
      +I18n.t("Ctrl+Alt+1…4: abrir ou fechar cada painel; Ctrl+Alt+0: ocultar todos.\n")
      +I18n.t("Esc: fechar o painel com foco.\n\n")
      +I18n.t("Todas as ferramentas originais estão no símbolo Ferramentas.\n")
      +I18n.t("A Biblioteca reúne todas as bibliotecas carregadas; Legacy mantém a lista nativa.\n")
      +I18n.t("A tabela integrada atende circuitos combinacionais."),
      I18n.t("Como usar"),JOptionPane.INFORMATION_MESSAGE);
  }
  private JComponent buildStatus(){
    zoomOverlay.setBackground(Color.WHITE);

    ZoomModel model=frame.getZoomModel();model.setShowGrid(Boolean.parseBoolean(saved.getProperty("grid","true")));
    JButton percent=button("100%",I18n.t("Voltar ao zoom 100%"),() -> model.setZoomFactorCenter(1.0));
    zoomOverlay.add(button("−",I18n.t("Diminuir zoom"),() -> model.setZoomFactorCenter(Math.max(.2,model.getZoomFactor()/1.2))));
    zoomOverlay.add(percent);
    zoomOverlay.add(button("+",I18n.t("Aumentar zoom"),() -> model.setZoomFactorCenter(Math.min(8,model.getZoomFactor()*1.2))));
    zoomOverlay.add(button(I18n.t("Enquadrar"),I18n.t("Ajustar o circuito ao espaço disponível"),this::fitCircuit));
    model.addPropertyChangeListener(ZoomModel.ZOOM,e -> percent.setText(Math.round(model.getZoomFactor()*100)+"%"));
    percent.setText(Math.round(model.getZoomFactor()*100)+"%");
    JCheckBox grid=I18n.check(I18n.t("Grade"),model.getShowGrid());grid.setOpaque(false);
    grid.addActionListener(e -> model.setShowGrid(grid.isSelected()));zoomOverlay.add(grid);
    feedback.setFont(UiFonts.font("Segoe UI",Font.PLAIN,11));feedback.setForeground(new Color(99,113,134));
    feedback.setToolTipText(feedback.getText());surface.add(feedback,JLayeredPane.PALETTE_LAYER);return zoomOverlay;
  }
  private static void styleAttributes(AttrTable attributes){
    JTable table=LibraryPanel.find(attributes,JTable.class);if(table==null)return;
    table.setRowHeight(34);table.setIntercellSpacing(new Dimension(6,5));table.setShowGrid(false);
    table.setDefaultRenderer(Object.class,new javax.swing.table.DefaultTableCellRenderer(){
      public Component getTableCellRendererComponent(JTable t,Object v,boolean selected,boolean focus,int r,int c){
        super.getTableCellRendererComponent(t,v,selected,focus,r,c);
        setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));setForeground(new Color(35,48,67));
        setBackground(selected?new Color(235,244,255):Color.WHITE);
        setBorder(c==1?new CompoundBorder(new LineBorder(new Color(220,227,237),1,true),new EmptyBorder(2,7,2,7)):new EmptyBorder(2,5,2,5));
        setToolTipText(v==null?null:v.toString());return this;
      }
    });
  }
  static Tool findTool(Library lib,String name,Set<Library> visited) {
    if(!visited.add(lib))return null;
    for(Tool tool:lib.getTools())if(name.equals(tool.getName()))return tool;
    for(Library child:lib.getLibraries()){
      Tool found=findTool(child,name,visited);if(found!=null)return found;
    }return null;
  }
  private void addPanel(String id,String title,JComponent content,Dimension minimum) {
    FloatingPanel panel=new FloatingPanel(this,id,title,content,minimum);
    panels.put(id,panel);surface.add(panel,JLayeredPane.PALETTE_LAYER);
    panel.setVisible(false);
  }
  private void bindKeys() {
    InputMap keys=getInputMap(WHEN_IN_FOCUSED_WINDOW);ActionMap actions=getActionMap();
    int i=1;
    for(String id:panels.keySet()){
      String key="panel-"+id;keys.put(KeyStroke.getKeyStroke("ctrl alt "+i++),key);
      actions.put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){
        if(panels.get(id).isOpen())hide(id);else show(id);
      }});
    }
    keys.put(KeyStroke.getKeyStroke("ctrl alt 0"),"focus-canvas");
    actions.put("focus-canvas",new AbstractAction(){public void actionPerformed(ActionEvent e){
      panels.keySet().forEach(ModernWorkspace.this::hide);
    }});
  }
  void show(String id) {
    FloatingPanel panel=panels.get(id);if(panel==null)return;
    panel.animateVisible(true);toggles.get(id).setSelected(true);
    constrain(panel);bringToFront(panel);
    if(id.equals("simulation")){simulation.rebuildPins();simulation.refreshValues();}
    if(id.equals("truth"))truth.ensureCalculated();
    scheduleSave();
  }
  void hide(String id) {
    FloatingPanel panel=panels.get(id);if(panel==null)return;
    panel.animateVisible(false);toggles.get(id).setSelected(false);
    frame.getCanvas().requestFocusInWindow();scheduleSave();
  }
  void bringToFront(FloatingPanel panel) {surface.moveToFront(panel);}
  void arrangeSurface() {
    nativeView.setBounds(0,0,surface.getWidth(),surface.getHeight());
    if(surface.getWidth()<100 || surface.getHeight()<100)return;
    if(!initialized){
      initialized=true;defaultLayout();
      if(saved.getProperty("version","").equals("2"))restoreLayout(saved);
      if(panels.get("truth").isVisible())truth.ensureCalculated();
    }
    for(FloatingPanel panel:panels.values())constrain(panel);
    toolbar.ensureCapacity();
    Dimension zoomSize=zoomOverlay.getPreferredSize();
    zoomOverlay.setBounds((surface.getWidth()-zoomSize.width)/2,surface.getHeight()-zoomSize.height-12,zoomSize.width,zoomSize.height);
    feedback.setBounds(Math.max(8,surface.getWidth()-230),surface.getHeight()-30,215,22);
    scheduleSave();
  }
  void defaultLayout() {
    int w=Math.max(700,surface.getWidth()),h=Math.max(450,surface.getHeight());
    int left=Math.min(380,(int)(w*.24)),right=Math.min(380,(int)(w*.24));
    int upper=Math.min(640,Math.max(360,h-305));
    setPanel("library",12,12,left,upper,true);
    setPanel("properties",w-right-12,12,right,Math.min(560,upper),true);
    setPanel("simulation",12,h-300,Math.min(620,w/2-32),260,true);
    setPanel("truth",w-Math.min(610,w/2-32)-12,h-412,Math.min(610,w/2-32),370,true);
    toolbar.setBounds(Math.max(left+32,(w-690)/2),35,690,72);toolbar.ensureCapacity();
    scheduleSave();
  }
  private void setPanel(String id,int x,int y,int w,int h,boolean visible){
    FloatingPanel panel=panels.get(id);
    if(panel.minimized)panel.setMinimized(false);
    panel.setBounds(x,y,w,h);panel.expandedHeight=h;
    panel.setVisible(visible);toggles.get(id).setSelected(visible);constrain(panel);
  }
  void constrain(FloatingPanel panel) {
    int w=surface.getWidth(),h=surface.getHeight();if(w<80||h<80)return;
    int pw=Math.min(Math.max(Math.min(panel.minimum.width,w-16),panel.getWidth()),w-16);
    int ph=Math.min(Math.max(panel.minimized?52:Math.min(panel.minimum.height,h-16),
        panel.getHeight()),h-16);
    int x=Math.max(8,Math.min(panel.getX(),w-pw-8));
    int y=Math.max(8,Math.min(panel.getY(),h-ph-8));
    panel.setBounds(x,y,pw,ph);
  }
  void scheduleSave() {if(initialized && !retired)saveTimer.restart();}
  void constrainToolbar(){
    int w=surface.getWidth(),h=surface.getHeight();if(w<80||h<80)return;
    int tw=Math.min(toolbar.getWidth(),w-16),th=Math.min(toolbar.getHeight(),h-16);
    toolbar.setBounds(Math.max(8,Math.min(toolbar.getX(),w-tw-8)),Math.max(8,Math.min(toolbar.getY(),h-th-8)),tw,th);
  }
  Properties captureLayout() {
    Properties p=new Properties();p.setProperty("version","2");
    p.setProperty("viewport.width",""+surface.getWidth());
    p.setProperty("viewport.height",""+surface.getHeight());
    p.setProperty("grid",""+frame.getZoomModel().getShowGrid());
    p.setProperty("toolbar.x",""+toolbar.getX());p.setProperty("toolbar.y",""+toolbar.getY());
    p.setProperty("toolbar.w",""+toolbar.getWidth());p.setProperty("toolbar.h",""+toolbar.getHeight());
    for(FloatingPanel panel:panels.values()){
      String k=panel.id+".";
      p.setProperty(k+"x",""+panel.getX());p.setProperty(k+"y",""+panel.getY());
      p.setProperty(k+"w",""+panel.getWidth());p.setProperty(k+"h",""+panel.getHeight());
      p.setProperty(k+"expanded",""+panel.expandedHeight);
      p.setProperty(k+"visible",""+panel.isOpen());p.setProperty(k+"minimized",""+panel.minimized);
    }return p;
  }
  void restoreLayout(Properties p){
    double sx=surface.getWidth()/(double)Math.max(1,number(p,"viewport.width",surface.getWidth()));
    double sy=surface.getHeight()/(double)Math.max(1,number(p,"viewport.height",surface.getHeight()));
    if(p.containsKey("toolbar.x")){
      toolbar.setBounds((int)(number(p,"toolbar.x",32)*sx),(int)(number(p,"toolbar.y",32)*sy),number(p,"toolbar.w",690),number(p,"toolbar.h",72));toolbar.ensureCapacity();
    }
    for(FloatingPanel panel:panels.values()){
      String k=panel.id+".";
      if(!p.containsKey(k+"x"))continue;
      panel.setBounds((int)(number(p,k+"x",12)*sx),(int)(number(p,k+"y",12)*sy),
          number(p,k+"w",300),number(p,k+"h",350));
      panel.expandedHeight=number(p,k+"expanded",350);
      boolean minimized=Boolean.parseBoolean(p.getProperty(k+"minimized","false"));
      if(minimized!=panel.minimized){
        if(minimized){
          int expanded=panel.expandedHeight;panel.setMinimized(true);panel.expandedHeight=expanded;
        }else panel.setMinimized(false);
      }
      panel.setVisible(Boolean.parseBoolean(p.getProperty(k+"visible","true")));
      toggles.get(panel.id).setSelected(panel.isVisible());constrain(panel);
    }
  }
  static int number(Properties p,String key,int fallback){
    try{return Integer.parseInt(p.getProperty(key));}catch(RuntimeException e){return fallback;}
  }
  void saveLayout(){
    if(!initialized||retired)return;
    Path temp=layoutFile.resolveSibling(layoutFile.getFileName()+".tmp");
    try {
      try(OutputStream out=Files.newOutputStream(temp)){captureLayout().store(out,"Logisim floating workspace");}
      try{Files.move(temp,layoutFile,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
      catch(AtomicMoveNotSupportedException e){Files.move(temp,layoutFile,StandardCopyOption.REPLACE_EXISTING);}
    }catch(IOException e){hint(I18n.t("Não foi possível salvar a disposição."));e.printStackTrace();}
  }
  private void updateHeader(){
    propertiesHint.setVisible(nativeAttributes.getAttrTableModel().getRowCount()==0);
    positionInfo.setVisible(false);
    var selection=project.getSelection().getComponents();
    if(selection.size()==1){
      var component=selection.iterator().next();
      positionInfo.setText(I18n.t("Posição  X: ")+component.getLocation().getX()+"   Y: "+component.getLocation().getY());
    }
    projectName.setText(project.getCurrentCircuit()==null?I18n.t("Circuito"):project.getCurrentCircuit().getName()
        +(project.isFileDirty()?I18n.t(" • alterado"):""));
    String state=project.getSimulator().isAutoPropagating()?I18n.t("Simulação ativa"):I18n.t("Simulação pausada");
    if(project.getSimulator().isOscillating())state=I18n.t("Oscilação detectada");
    else if(project.getSimulator().isExceptionEncountered())state=I18n.t("Erro na simulação");
    status.setText(state);
  }
  void refreshLanguage(){
    I18n.refresh(this);toolbar.refreshLanguage();
    library.quickTitle.getAccessibleContext().setAccessibleName(I18n.t(library.favorites.isVisible()?"Recolher":"Expandir")+I18n.t(" Acesso rápido"));
    library.listedFile=null;library.refresh();
    propertyForm.signature="";propertyForm.refresh();simulation.invalidatePins();truth.refreshLanguage();
    updateHeader();arrangeSurface();revalidate();repaint();
  }
  void hint(String text){feedback.putClientProperty("i18n.text",null);feedback.setText(text);}
  void fitCircuit(){
    var circuit=project.getCurrentCircuit();if(circuit==null)return;
    var bounds=circuit.getBounds();if(bounds.getWidth()<=0||bounds.getHeight()<=0)return;
    int width=surface.getWidth(),height=surface.getHeight();
    double left=24,right=width-24,bottom=height-65,top=toolbar.getHeight()<180?toolbar.getY()+toolbar.getHeight()+28:24;
    FloatingPanel library=panels.get("library"),properties=panels.get("properties");
    if(library.isVisible() && !library.minimized && library.getX()<width*.25)
      left=library.getX()+library.getWidth()+24;
    if(properties.isVisible() && !properties.minimized && properties.getX()>width*.6)
      right=properties.getX()-24;
    for(String id:new String[]{"simulation","truth"}){
      FloatingPanel panel=panels.get(id);
      if(panel.isVisible()&&!panel.minimized&&panel.getY()>height*.4)bottom=Math.min(bottom,panel.getY()-24);
    }
    double availableWidth=Math.max(200,right-left),availableHeight=Math.max(180,bottom-top);
    double zoom=Math.max(.2,Math.min(2.5,Math.min(availableWidth/(bounds.getWidth()+90.0),
        availableHeight/(bounds.getHeight()+80.0))));
    frame.getZoomModel().setZoomFactorCenter(zoom);
    double centerX=left+availableWidth/2,centerY=top+availableHeight/2;
    SwingUtilities.invokeLater(() -> {
      var pane=frame.getCanvas().getCanvasPane();
      pane.getHorizontalScrollBar().setValue((int)Math.max(0,
          (bounds.getX()+bounds.getWidth()/2.0)*zoom-centerX));
      pane.getVerticalScrollBar().setValue((int)Math.max(0,
          (bounds.getY()+bounds.getHeight()/2.0)*zoom-centerY));
    });
  }
  void dispose(){
    if(retired)return;saveLayout();retired=true;saveTimer.stop();statusTimer.stop();
    Appearance.workspaces.remove(this);panels.values().forEach(FloatingPanel::dispose);
    project.removeProjectListener(projectListener);simulation.dispose();truth.dispose();toolbar.dispose();
  }
  void restoreOriginal(){
    dispose();
    canvasSkin.restore();
    library.restoreNative();menuDecorations.forEach(nativeMenu::remove);
    propertyForm.original.setVisible(true);attributesTabs.setComponentAt(0,propertyForm.original);
    Collections.reverse(slots);for(Slot slot:slots)slot.restore();
    frame.getToolbar().setOrientation(Toolbar.HORIZONTAL);
    frame.setContentPane(original);frame.getRootPane().putClientProperty("floating.workspace",Boolean.FALSE);
    frame.revalidate();frame.repaint();
  }
  static JButton button(String text,String tip,Runnable action){
    JButton b=I18n.button(text);I18n.tip(b,tip);
    String key=text.replaceAll("[^\\p{L}]","");
    if(key.equals(I18n.t("Executar")))b.setIcon(new UiIcon("play"));
    else if(text.equals(I18n.t("Pausar")))b.setIcon(new UiIcon("pause"));
    else if(key.equals(I18n.t("Passo")))b.setIcon(new UiIcon("step"));
    else if(key.equals(I18n.t("Reiniciar")))b.setIcon(new UiIcon("reset"));
    b.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));
    if(action!=null)b.addActionListener(e -> action.run());return b;
  }
  private static void item(JPopupMenu menu,String text,Runnable action){
    JMenuItem item=I18n.item(text);item.addActionListener(e -> action.run());menu.add(item);
  }
  static void polish(Component component){

    if(component instanceof JTable table){table.setRowHeight(24);}
    if(component instanceof JTree tree){
      tree.setFont(UiFonts.font("Segoe UI",Font.PLAIN,13));tree.setRowHeight(25);
      javax.swing.tree.TreeCellRenderer delegate=tree.getCellRenderer();
      tree.setCellRenderer((t,v,s,e,l,r,f) -> {
        Component cell=delegate.getTreeCellRendererComponent(t,v,s,e,l,r,f);
        cell.setFont(UiFonts.font("Segoe UI",Font.PLAIN,13));return cell;
      });
    }
    if(component instanceof Container container)
      for(Component child:container.getComponents())polish(child);
  }
  static final class Slot {
    final JComponent component;final Container parent;final Object constraint;final int index;
    Slot(JComponent c){
      component=c;parent=c.getParent();int idx=0;Object cons=null;
      if(parent!=null){
        for(Component sibling:parent.getComponents()){if(sibling==c)break;idx++;}
        if(parent.getLayout() instanceof BorderLayout border)cons=border.getConstraints(c);
      }index=idx;constraint=cons;
    }
    void restore(){
      if(parent==null)return;
      if(component.getParent()!=null)component.getParent().remove(component);
      if(constraint!=null)parent.add(component,constraint);
      else parent.add(component,Math.min(index,parent.getComponentCount()));
      parent.revalidate();
    }
  }
  static final class ToolIcon implements Icon {
    final Tool tool;final Project project;
    ToolIcon(Tool tool,Project project){this.tool=tool;this.project=project;}
    public int getIconWidth(){return 36;}public int getIconHeight(){return 36;}
    public void paintIcon(Component c,Graphics g,int x,int y){
      Graphics2D copy=(Graphics2D)g.create();
      copy.translate(x+4,y+4);copy.scale(1.8,1.8);
      copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
      tool.paintIcon(new ComponentDrawContext(c,project.getCurrentCircuit(),project.getCircuitState(),
          copy,copy),0,0);copy.dispose();
    }
  }
}
