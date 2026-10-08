package local.logisim.panels;

import com.cburch.logisim.gui.generic.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/** Fields commit through the native attribute rows, including their undo actions. */
final class PropertiesForm extends JPanel {
  final ModernWorkspace owner;
  final AttrTable attributes;
  final JComponent original;
  final JPanel cards=new JPanel();
  final JPanel all=new JPanel(new BorderLayout());
  final List<Binding> bindings=new ArrayList<>();
  String signature="";
  boolean syncing;
  PropertiesForm(ModernWorkspace owner,AttrTable attributes,JComponent original){
    super(new BorderLayout());this.owner=owner;this.attributes=attributes;this.original=original;
    setBackground(Color.WHITE);cards.setBackground(Color.WHITE);cards.setLayout(new BoxLayout(cards,BoxLayout.Y_AXIS));
    JPanel content=new JPanel(new BorderLayout(0,7));content.setBackground(Color.WHITE);
    content.add(cards,BorderLayout.NORTH);
    JButton expand=ModernWorkspace.button(I18n.t("›  Todas as propriedades"),I18n.t("Mostrar a tabela completa de atributos e seus editores"),null);
    expand.setHorizontalAlignment(SwingConstants.LEFT);expand.putClientProperty("JButton.buttonType","borderless");
    all.setOpaque(false);all.add(expand,BorderLayout.NORTH);all.add(original,BorderLayout.CENTER);original.setVisible(false);
    expand.addActionListener(e -> {boolean visible=!original.isVisible();original.setVisible(visible);expand.setText((visible?"⌄  ":"›  ")+I18n.t("Todas as propriedades"));revalidate();});
    content.add(all,BorderLayout.CENTER);
    JScrollPane scroll=new JScrollPane(content);scroll.setBorder(new EmptyBorder(0,0,0,0));scroll.getVerticalScrollBar().setUnitIncrement(26);add(scroll);
  }
  void refresh(){
    AttrTableModel model=attributes.getAttrTableModel();
    String key=model.getTitle()+":"+model.getRowCount();
    for(int i=0;i<model.getRowCount();i++)key+="|"+model.getRow(i).getLabel();
    if(!signature.equals(key)){signature=key;build(model);}
    syncing=true;
    try{for(Binding b:bindings){
      String value=b.row.getValue();
      if(b.editor instanceof JTextField field && !field.hasFocus()&&!field.getText().equals(value))field.setText(value);
      else if(b.editor instanceof JButton button&&!button.getText().equals(value))button.setText(value);
      else if(b.editor instanceof JComboBox<?> combo && !combo.isPopupVisible()&&!combo.hasFocus()){
        for(int i=0;i<combo.getItemCount();i++)if(String.valueOf(combo.getItemAt(i)).equals(value)){combo.setSelectedIndex(i);break;}
      }
    }}finally{syncing=false;}
  }
  void build(AttrTableModel model){
    cards.removeAll();bindings.clear();if(model.getRowCount()==0){cards.revalidate();cards.repaint();return;}
    JPanel component=card(model.getTitle()),labels=card(I18n.t("Rótulo"));boolean hasLabels=false;
    var selection=owner.project.getSelection().getComponents();
    if(selection.size()==1){var location=selection.iterator().next().getLocation();
      addReadOnly(component,I18n.t("Posição X"),""+location.getX());addReadOnly(component,I18n.t("Posição Y"),""+location.getY());}
    for(int i=0;i<model.getRowCount();i++){
      final int index=i;AttrTableModelRow row=model.getRow(i);String name=row.getLabel();
      if(name.contains("FPGA"))continue;
      boolean label=name.toLowerCase(Locale.ROOT).contains(I18n.t("Rótulo").toLowerCase(Locale.ROOT));
      // Less common settings remain in the full native editor underneath.
      if(!(label||i<7))continue;
      JPanel group=label?labels:component;if(label)hasLabels=true;
      String title=(name.equals("Posição")||name.equals("Facing"))?I18n.t("Orientação"):name;
      if(label)title=name.equals(I18n.t("Rótulo"))?I18n.t("Texto"):name.replace("do rótulo","").trim();
      JComponent editor;
      if(!row.isValueEditable()){
        JTextField field=new JTextField(row.getValue());field.setEditable(false);editor=field;
      }else {
        Component nativeEditor=row.getEditor(owner.frame);
        if(nativeEditor instanceof JComboBox<?> combo){
          editor=combo;combo.addActionListener(e -> {if(!syncing)commit(row,combo.getSelectedItem());});
        }else if(nativeEditor instanceof JTextField field){
          field.setText(row.getValue());editor=field;
          field.addActionListener(e -> commit(row,field.getText()));
          field.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){if(!syncing&&!field.getText().equals(row.getValue()))commit(row,field.getText());}});
        }else {
          JButton button=ModernWorkspace.button(row.getValue(),I18n.t("Editar ")+name+I18n.t(" no editor completo"),() -> editNative(index));editor=button;
        }
      }
      editor.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));editor.setToolTipText(name);editor.setPreferredSize(new Dimension(145,30));
      editor.getAccessibleContext().setAccessibleName(name);addRow(group,title,editor);bindings.add(new Binding(row,editor));
    }
    cards.add(component);if(hasLabels){cards.add(Box.createVerticalStrut(9));cards.add(labels);}cards.add(Box.createVerticalStrut(7));
    cards.revalidate();cards.repaint();
  }
  JPanel card(String title){
    JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(Color.WHITE);
    p.setBorder(new CompoundBorder(new LineBorder(new Color(221,229,240),1,true),new EmptyBorder(7,8,8,8)));
    JLabel caption=new JLabel(title);caption.setFont(UiFonts.font("Segoe UI",Font.BOLD,12));caption.setBorder(new EmptyBorder(2,0,7,0));
    caption.setAlignmentX(LEFT_ALIGNMENT);p.add(caption);p.setAlignmentX(LEFT_ALIGNMENT);return p;
  }
  void addReadOnly(JPanel panel,String label,String value){JTextField field=new JTextField(value);field.setEditable(false);I18n.tip(field,I18n.t("Arraste o componente na grade para alterar sua posição."));addRow(panel,label,field);}
  void addRow(JPanel panel,String label,JComponent editor){
    JPanel row=new JPanel(new BorderLayout(8,0));row.setOpaque(false);row.setBorder(new EmptyBorder(3,0,3,0));
    JLabel name=new JLabel(label);name.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));name.setToolTipText(label);name.setPreferredSize(new Dimension(126,30));
    row.add(name,BorderLayout.WEST);row.add(editor,BorderLayout.CENTER);row.setAlignmentX(LEFT_ALIGNMENT);panel.add(row);
  }
  void commit(AttrTableModelRow row,Object value){
    try{row.setValue(owner.frame,value);owner.project.repaintCanvas();}
    catch(Exception e){if(!owner.frame.isShowing())throw new IllegalArgumentException(I18n.t("Não foi possível editar ")+row.getLabel(),e);
      JOptionPane.showMessageDialog(owner.frame,e.getMessage(),I18n.t("Propriedade"),JOptionPane.INFORMATION_MESSAGE);}
  }
  void editNative(int row){
    original.setVisible(true);JTable table=LibraryPanel.find(attributes,JTable.class);revalidate();
    if(table!=null){table.scrollRectToVisible(table.getCellRect(row,1,true));table.editCellAt(row,1);if(table.getEditorComponent()!=null)table.getEditorComponent().requestFocusInWindow();}
  }
  record Binding(AttrTableModelRow row,JComponent editor){}
}
