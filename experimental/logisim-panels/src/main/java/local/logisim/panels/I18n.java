package local.logisim.panels;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Only controls created by this interface are bound; circuit names and native editors are untouched. */
final class I18n {
  static String language="en";
  static final Map<String,String> english=new LinkedHashMap<>();
  static {
    try(var in=I18n.class.getResourceAsStream("en.tsv")){
      if(in==null)throw new IOException("Missing language resource");
      try(var reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
        for(String line;(line=reader.readLine())!=null;){
          int tab=line.indexOf('\t');
          if(tab>=0)english.put(unescape(line.substring(0,tab)),unescape(line.substring(tab+1)));
        }
      }
    }catch(IOException e){throw new ExceptionInInitializerError(e);}
  }
  static String unescape(String text){return text.replace("\\n","\n");}
  static String t(String key){return language.equals("pt-BR")?key:english.getOrDefault(key,key);}
  static Locale locale(){return language.equals("pt-BR")?Locale.forLanguageTag("pt-BR"):Locale.US;}
  static String key(String displayed){
    if(english.containsKey(displayed))return displayed;
    for(var entry:english.entrySet())if(entry.getValue().equals(displayed))return entry.getKey();
    return null;
  }
  static <T extends JComponent> T bind(T component){
    String text=component instanceof JLabel l?l.getText():component instanceof AbstractButton b?b.getText():null;
    String key=key(text);if(key!=null)component.putClientProperty("i18n.text",key);
    return component;
  }
  static JLabel label(String text){return bind(new JLabel(text));}
  static JLabel label(String text,Icon icon,int alignment){return bind(new JLabel(text,icon,alignment));}
  static JLabel label(String text,int alignment){return bind(new JLabel(text,alignment));}
  static JButton button(String text){return bind(new JButton(text));}
  static JButton button(String text,Icon icon){return bind(new JButton(text,icon));}
  static JCheckBox check(String text){return bind(new JCheckBox(text));}
  static JCheckBox check(String text,boolean selected){return bind(new JCheckBox(text,selected));}
  static JMenuItem item(String text){return bind(new JMenuItem(text));}
  static void tip(JComponent component,String text){
    component.setToolTipText(text);component.putClientProperty("i18n.tip",key(text));
  }
  static void placeholder(JComponent component,String text){
    component.putClientProperty("JTextField.placeholderText",text);
    component.putClientProperty("i18n.placeholder",key(text));
  }
  static void tab(JTabbedPane pane,String text,Component child){
    pane.addTab(text,child);String key=key(text);
    if(child instanceof JComponent c&&key!=null)c.putClientProperty("i18n.tab",key);
  }
  static void refresh(Component component){
    if(component instanceof JComponent c){
      boolean owned=c.getClientProperty("i18n.text")!=null||c.getClientProperty("i18n.tip")!=null||c.getClientProperty("i18n.placeholder")!=null;
      String accessibleKey=owned?key(c.getAccessibleContext().getAccessibleName()):null;
      if(c.getClientProperty("i18n.text") instanceof String key){
        if(c instanceof JLabel label)label.setText(t(key));
        else if(c instanceof AbstractButton button)button.setText(t(key));
      }
      if(c.getClientProperty("i18n.tip") instanceof String key)c.setToolTipText(t(key));
      if(c.getClientProperty("i18n.placeholder") instanceof String key)c.putClientProperty("JTextField.placeholderText",t(key));
      if(accessibleKey!=null)c.getAccessibleContext().setAccessibleName(t(accessibleKey));
      if(c instanceof JTabbedPane pane)for(int i=0;i<pane.getTabCount();i++){
        if(pane.getComponentAt(i) instanceof JComponent child&&child.getClientProperty("i18n.tab") instanceof String key)pane.setTitleAt(i,t(key));
      }
    }
    if(component instanceof Container container)for(Component child:container.getComponents())refresh(child);
  }
}
