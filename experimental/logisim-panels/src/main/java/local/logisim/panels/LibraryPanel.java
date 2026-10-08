package local.logisim.panels;

import com.cburch.draw.toolbar.*;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.main.LogisimToolbarItem;
import com.cburch.logisim.tools.*;
import com.cburch.logisim.std.wiring.Pin;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/** Native project explorer is kept intact; the component view covers every loaded library. */
final class LibraryPanel extends JPanel {
  final ModernWorkspace owner;
  final JComponent nativeToolbox;
  final ProjectExplorer explorer;
  final Toolbar management;
  final JTextField search=new JTextField();
  final JTabbedPane tabs=new JTabbedPane();
  final JPanel categories=new JPanel();
  final JPanel favorites=new JPanel(new GridLayout(2,3,5,5));
  final JButton quickTitle=I18n.button(I18n.t("Acesso rápido"),new UiIcon("chevron",14));
  final JLabel active=new JLabel();
  final Set<String> expanded=new HashSet<>();
  final List<JComponent> hiddenNative=new ArrayList<>();
  final List<Tool> accessibleTools=new ArrayList<>();
  Library listedFile;
  int structureHash;
  long nextStructureCheck;
  LibraryPanel(ModernWorkspace owner,JComponent nativeToolbox){
    super(new BorderLayout(0,9));this.owner=owner;this.nativeToolbox=nativeToolbox;
    setBackground(Color.WHITE);
    explorer=find(nativeToolbox,ProjectExplorer.class);management=find(nativeToolbox,Toolbar.class);
    if(management!=null){management.setVisible(false);hiddenNative.add(management);}
    hideNativeFilter(nativeToolbox);
    I18n.placeholder(search,I18n.t("Buscar componente…"));
    search.putClientProperty("JTextField.leadingIcon",new UiIcon("search",17));
    I18n.tip(search,I18n.t("Pesquisar componentes em todas as bibliotecas carregadas"));
    search.getAccessibleContext().setAccessibleName(I18n.t("Buscar componente"));
    search.setPreferredSize(new Dimension(100,35));add(search,BorderLayout.NORTH);
    JPanel components=new JPanel(new BorderLayout(0,8));components.setBackground(Color.WHITE);
    JPanel scrollContent=new JPanel(new BorderLayout(0,8));scrollContent.setBackground(Color.WHITE);
    JPanel quick=new JPanel(new BorderLayout(0,5));quick.setOpaque(false);
    quickTitle.setFont(UiFonts.font("Segoe UI",Font.BOLD,12));
    quickTitle.setHorizontalAlignment(SwingConstants.LEFT);
    quickTitle.putClientProperty("JButton.buttonType","borderless");
    I18n.tip(quickTitle,I18n.t("Recolher ou expandir o Acesso rápido"));
    quickTitle.getAccessibleContext().setAccessibleName(I18n.t("Recolher Acesso rápido"));
    quickTitle.addActionListener(e -> {
      boolean show=!favorites.isVisible();favorites.setVisible(show);
      quickTitle.setIcon(new UiIcon(show?"chevron":"chevron-right",14));
      quickTitle.getAccessibleContext().setAccessibleName((show?I18n.t("Recolher"):I18n.t("Expandir"))+I18n.t(" Acesso rápido"));
      if(show){scrollContent.remove(quick);components.add(quick,BorderLayout.NORTH);}
      else {components.remove(quick);scrollContent.add(quick,BorderLayout.NORTH);}
      scrollContent.revalidate();scrollContent.repaint();
      components.revalidate();components.repaint();
    });
    quick.add(quickTitle,BorderLayout.NORTH);quick.add(favorites,BorderLayout.CENTER);
    components.add(quick,BorderLayout.NORTH);
    categories.setLayout(new BoxLayout(categories,BoxLayout.Y_AXIS));categories.setBackground(Color.WHITE);
    scrollContent.add(categories,BorderLayout.CENTER);
    JScrollPane scroll=new JScrollPane(scrollContent);scroll.setBorder(new EmptyBorder(0,0,0,0));scroll.getVerticalScrollBar().setUnitIncrement(22);
    components.add(scroll,BorderLayout.CENTER);
    I18n.tab(tabs,I18n.t("Componentes"),components);tabs.addTab("Legacy",nativeToolbox);
    tabs.setToolTipTextAt(1,I18n.t("Interface antiga de circuitos e bibliotecas do Logisim"));
    tabs.setOpaque(false);add(tabs,BorderLayout.CENTER);
    JPanel bottom=new JPanel(new BorderLayout(5,4));bottom.setOpaque(false);
    bottom.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,new Color(224,231,240)),new EmptyBorder(6,0,0,0)));
    JLabel caption=I18n.label(I18n.t("Circuito ativo"));caption.setFont(UiFonts.font("Segoe UI",Font.BOLD,11));
    bottom.add(caption,BorderLayout.NORTH);
    active.setIcon(new UiIcon("tools",24));active.setFont(UiFonts.font("Segoe UI",Font.BOLD,13));bottom.add(active,BorderLayout.CENTER);
    JButton manage=ModernWorkspace.button(I18n.t("Gerenciar"),I18n.t("Comandos completos de circuitos e VHDL"),null);
    manage.setIcon(new UiIcon("chevron",14));manage.setHorizontalTextPosition(SwingConstants.LEFT);
    manage.addActionListener(e -> managementMenu().show(manage,0,manage.getHeight()));bottom.add(manage,BorderLayout.EAST);
    add(bottom,BorderLayout.SOUTH);
    search.getDocument().addDocumentListener(new DocumentListener(){
      public void insertUpdate(DocumentEvent e){filter();}public void removeUpdate(DocumentEvent e){filter();}public void changedUpdate(DocumentEvent e){filter();}
    });
    refresh();
  }
  static <T> T find(Component c,Class<T> type){
    if(type.isInstance(c))return type.cast(c);
    if(c instanceof Container container)for(Component child:container.getComponents()){T found=find(child,type);if(found!=null)return found;}
    return null;
  }
  void hideNativeFilter(Container c){
    for(Component child:c.getComponents()){
      if(child instanceof JLabel label && ("Filter:".equals(label.getText())||"Buscar:".equals(label.getText()))){
        if(label.getParent() instanceof JComponent parent){hiddenNative.add(parent);parent.setVisible(false);}return;
      }
      if(child instanceof Container nested)hideNativeFilter(nested);
    }
  }
  void filter(){if(explorer!=null)explorer.setFilterText(search.getText());renderCategories();}
  void refresh(){
    active.setText(owner.project.getCurrentCircuit()==null?"VHDL":owner.project.getCurrentCircuit().getName());
    Library file=owner.project.getLogisimFile();
    long now=System.nanoTime();
    if(file==listedFile&&now<nextStructureCheck)return;
    nextStructureCheck=now+1_000_000_000L;
    int hash=hash(file,new HashSet<>());
    if(file==listedFile&&hash==structureHash)return;listedFile=file;structureHash=hash;
    favorites.removeAll();
    String[] names={"AND Gate","OR Gate","NOT Gate","XOR Gate","Pin","Pin"};
    String[] titles={"AND","OR","NOT","XOR",I18n.t("Entrada"),I18n.t("Saída")};
    String[] icons={"and","or","not","xor","input","output"};
    for(int i=0;i<names.length;i++){
      Tool tool=ModernWorkspace.findTool(file,names[i],new HashSet<>());if(tool==null)continue;
      final Tool copy=tool.cloneTool();if(names[i].equals("Pin"))copy.getAttributeSet().setValue(Pin.ATTR_TYPE,i==5?Pin.OUTPUT:Pin.INPUT);
      JButton tile=new JButton(titles[i],new UiIcon(icons[i],40));tile.setBackground(Color.WHITE);
      tile.setVerticalTextPosition(SwingConstants.BOTTOM);tile.setHorizontalTextPosition(SwingConstants.CENTER);
      tile.setPreferredSize(new Dimension(78,76));tile.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));
      I18n.tip(tile,I18n.t("Selecionar ")+titles[i]+I18n.t(" e inserir na grade"));tile.addActionListener(e -> select(copy));favorites.add(tile);
    }
    renderCategories();revalidate();repaint();
  }
  int hash(Library lib,Set<Library> visited){
    if(!visited.add(lib))return 0;int h=System.identityHashCode(lib)*31+lib.getTools().size();
    for(Tool tool:lib.getTools())h=31*h+tool.getDisplayName().hashCode();
    for(Library child:lib.getLibraries())h=31*h+hash(child,visited);return h;
  }
  void renderCategories(){
    categories.removeAll();accessibleTools.clear();String query=search.getText().trim().toLowerCase(Locale.ROOT);
    List<Group> groups=new ArrayList<>();Library file=owner.project.getLogisimFile();
    for(Library lib:file.getLibraries())collectGroups(lib,lib.getDisplayName(),groups,new HashSet<>());
    if(!file.getTools().isEmpty())groups.add(new Group(I18n.t("Circuitos do projeto"),new ArrayList<>(file.getTools())));
    for(Group group:groups){
      accessibleTools.addAll(group.tools);List<Tool> matches=new ArrayList<>();
      for(Tool tool:group.tools)if(query.isEmpty()||tool.getDisplayName().toLowerCase(Locale.ROOT).contains(query)
          ||tool.getName().toLowerCase(Locale.ROOT).contains(query)||group.name.toLowerCase(Locale.ROOT).contains(query))matches.add(tool);
      if(matches.isEmpty())continue;
      boolean open=expanded.contains(group.name)||!query.isEmpty();
      JButton title=new JButton(categoryName(group.name)+" ("+matches.size()+")",new UiIcon(open?"chevron":"chevron-right",14));
      title.setHorizontalAlignment(SwingConstants.LEFT);title.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));
      title.putClientProperty("JButton.buttonType","borderless");title.setMaximumSize(new Dimension(Integer.MAX_VALUE,29));
      title.setPreferredSize(new Dimension(250,29));title.setAlignmentX(LEFT_ALIGNMENT);
      title.addActionListener(e -> {if(expanded.contains(group.name))expanded.remove(group.name);else expanded.add(group.name);renderCategories();});
      categories.add(title);
      if(open)for(Tool tool:matches){
        JButton entry=new JButton(tool.getDisplayName(),new ModernWorkspace.ToolIcon(tool,owner.project));
        entry.setHorizontalAlignment(SwingConstants.LEFT);entry.putClientProperty("JButton.buttonType","borderless");
        entry.setToolTipText(tool.getDescription());entry.setMaximumSize(new Dimension(Integer.MAX_VALUE,31));entry.setAlignmentX(LEFT_ALIGNMENT);
        entry.addActionListener(e -> select(tool.cloneTool()));categories.add(entry);
      }
    }
    if(categories.getComponentCount()==0){JLabel none=I18n.label(I18n.t("Nenhum componente encontrado."));none.setBorder(new EmptyBorder(8,3,8,3));categories.add(none);}
    categories.add(Box.createVerticalGlue());categories.revalidate();categories.repaint();
  }
  void collectGroups(Library lib,String name,List<Group> groups,Set<Library> visited){
    if(!visited.add(lib))return;
    if(!lib.getTools().isEmpty())groups.add(new Group(name,new ArrayList<>(lib.getTools())));
    for(Library child:lib.getLibraries())collectGroups(child,name+" / "+child.getDisplayName(),groups,visited);
  }
  String categoryName(String text){return (text.equals("Portas")||text.equals("Gates"))?I18n.t("Portas lógicas"):text.equals("I/O")?I18n.t("Entrada/Saída"):text;}
  void select(Tool tool){owner.project.setTool(tool);owner.frame.getCanvas().requestFocusInWindow();owner.toolbar.refreshSelection();owner.hint(I18n.t("Clique na grade para inserir ")+tool.getDisplayName()+".");}
  JPopupMenu managementMenu(){
    JPopupMenu menu=new JPopupMenu();
    String[] labels={I18n.t("Adicionar circuito"),I18n.t("Adicionar VHDL"),I18n.t("Mover para cima"),I18n.t("Mover para baixo"),I18n.t("Editar aparência"),I18n.t("Remover")};
    if(management!=null){int i=0;for(ToolbarItem item:management.getToolbarModel().getItems()){
      if(!(item instanceof LogisimToolbarItem action))continue;
      JMenuItem entry=new JMenuItem(i<labels.length?labels[i++]:item.getToolTip());entry.setToolTipText(item.getToolTip());
      entry.setEnabled(item.isSelectable());entry.addActionListener(e -> action.doAction());menu.add(entry);
    }}
    menu.addSeparator();JMenuItem properties=I18n.item(I18n.t("Renomear / propriedades do circuito"));
    properties.addActionListener(e -> {try{var method=owner.frame.getClass().getDeclaredMethod("viewCircuitAttributes");method.setAccessible(true);method.invoke(owner.frame);owner.show("properties");}
      catch(Exception failure){owner.hint(I18n.t("Abra as propriedades pelo menu Projeto."));failure.printStackTrace();}});menu.add(properties);
    JMenuItem complete=I18n.item(I18n.t("Lista completa de circuitos e bibliotecas"));complete.addActionListener(e -> tabs.setSelectedIndex(1));menu.add(complete);return menu;
  }
  void restoreNative(){hiddenNative.forEach(c -> c.setVisible(true));if(explorer!=null)explorer.setFilterText("");}
  record Group(String name,List<Tool> tools){}
}
