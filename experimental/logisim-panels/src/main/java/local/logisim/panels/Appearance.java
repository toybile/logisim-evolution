package local.logisim.panels;

import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

final class Appearance {
  record Palette(String name,Color color){public String toString(){return I18n.t(name);}}
  static final Palette[] THEMES={
    new Palette("Azul claro",new Color(243,248,255)),
    new Palette("Verde claro",new Color(239,248,243)),
    new Palette("Areia",new Color(252,246,234)),
    new Palette("Violeta clara",new Color(247,241,253)),
    new Palette("Cinza claro",new Color(245,246,248))
  };
  static final Palette[] COLORS={
    new Palette("Azul",new Color(0,112,225)),
    new Palette("Verde",new Color(0,155,78)),
    new Palette("Violeta",new Color(128,71,206)),
    new Palette("Laranja",new Color(192,104,0)),
    new Palette("Rosa",new Color(190,55,124)),
    new Palette("Turquesa",new Color(0,137,151)),
    new Palette("Grafite",new Color(79,91,111))
  };
  static int theme=0,selection=0,signal=1;
  static float opacity=.94f;
  static boolean animations=true;
  static Path file;
  static final java.util.List<ModernWorkspace> workspaces=new ArrayList<>();
  static Color accent(){return COLORS[selection].color();}
  static Color signalOne(){return COLORS[signal].color();}
  static Color canvas(){return THEMES[theme].color();}
  static Color mix(Color a,Color b,float amount){
    return new Color(Math.round(a.getRed()*(1-amount)+b.getRed()*amount),
      Math.round(a.getGreen()*(1-amount)+b.getGreen()*amount),
      Math.round(a.getBlue()*(1-amount)+b.getBlue()*amount));
  }
  static Color selectionBackground(){return mix(Color.WHITE,accent(),.13f);}
  static void load(Path path){
    file=path;Properties p=new Properties();
    try(InputStream in=Files.newInputStream(file)){p.load(in);}catch(IOException ignored){}
    theme=number(p,"theme",0,THEMES.length-1,0);
    selection=number(p,"selection",0,COLORS.length-1,0);
    signal=number(p,"signal",0,COLORS.length-1,1);
    opacity=number(p,"opacity",85,100,94)/100f;
    I18n.language="pt-BR".equals(p.getProperty("language"))?"pt-BR":"en";
    animations=Boolean.parseBoolean(p.getProperty("animations","true"));
    configureDefaults();
  }
  static int number(Properties p,String key,int min,int max,int fallback){
    try{return Math.max(min,Math.min(max,Integer.parseInt(p.getProperty(key))));}
    catch(Exception ignored){return fallback;}
  }
  static void configureDefaults(){
    Color accent=accent(),tint=selectionBackground();
    Map<String,String> defaults=new HashMap<>();
    if(com.formdev.flatlaf.FlatLaf.getGlobalExtraDefaults()!=null)
      defaults.putAll(com.formdev.flatlaf.FlatLaf.getGlobalExtraDefaults());
    defaults.put("@accentColor",String.format("#%02x%02x%02x",accent.getRed(),accent.getGreen(),accent.getBlue()));
    com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(defaults);
    for(String key:new String[]{"Component.focusColor","Component.accentColor","TabbedPane.underlineColor",
      "TabbedPane.focusColor","Button.default.background","ProgressBar.foreground","Slider.thumbColor",
      "CheckBox.icon.selectedBackground","CheckBox.icon.selectedBorderColor","RadioButton.icon.selectedBackground"})
      UIManager.put(key,accent);
    for(String key:new String[]{"Tree.selectionBackground","Table.selectionBackground","List.selectionBackground",
      "ToggleButton.selectedBackground","ComboBox.selectionBackground","MenuItem.selectionBackground",
      "Menu.selectionBackground","TextField.selectionBackground","TextArea.selectionBackground"})
      UIManager.put(key,tint);
    for(String key:new String[]{"Tree.selectionForeground","Table.selectionForeground","List.selectionForeground",
      "ToggleButton.selectedForeground","ComboBox.selectionForeground","MenuItem.selectionForeground",
      "Menu.selectionForeground","CheckBoxMenuItem.selectionForeground","RadioButtonMenuItem.selectionForeground",
      "TextField.selectionForeground","TextArea.selectionForeground"})UIManager.put(key,new Color(29,43,61));
  }
  static void apply(){
    configureDefaults();
    com.cburch.logisim.util.LocaleManager.setLocale(I18n.locale());
    // FlatLaf shares UI delegates and lazy borders; recreate them when the accent changes.
    if(UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLaf){
      try { UIManager.setLookAndFeel(UIManager.getLookAndFeel().getClass().getDeclaredConstructor().newInstance()); }
      catch(Exception failure){throw new IllegalStateException(I18n.t("Não foi possível atualizar o tema"),failure);}
      configureDefaults();
    }
    com.cburch.logisim.data.Value.trueColor=signalOne();
    for(ModernWorkspace ui:new ArrayList<>(workspaces)){
      ui.refreshLanguage();
      SwingUtilities.updateComponentTreeUI(ui.frame);
      ui.nativeMenu.setBackground(mix(Color.WHITE,canvas(),.7f));
      for(FloatingPanel panel:ui.panels.values()){
        panel.header.setBackground(mix(Color.WHITE,canvas(),.85f));panel.repaint();
      }
      ui.canvasSkin.refresh();ui.frame.getCanvas().repaint();ui.toolbar.repaint();ui.truth.table.repaint();
      ui.simulation.refreshValues();
    }
    Properties p=new Properties();p.setProperty("language",I18n.language);p.setProperty("theme",""+theme);p.setProperty("selection",""+selection);
    p.setProperty("signal",""+signal);p.setProperty("opacity",""+Math.round(opacity*100));p.setProperty("animations",""+animations);
    if(file!=null)try(OutputStream out=Files.newOutputStream(file)){p.store(out,"Logisim appearance");}
    catch(IOException failure){failure.printStackTrace();}
  }
  static JComboBox<Palette> choices(Palette[] palettes,int selected){
    JComboBox<Palette> box=new JComboBox<>(palettes);box.setSelectedIndex(selected);
    box.setRenderer(new DefaultListCellRenderer(){
      public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
        JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
        if(value instanceof Palette palette)label.setIcon(new Icon(){
          public int getIconWidth(){return 19;}public int getIconHeight(){return 19;}
          public void paintIcon(Component c,Graphics g,int x,int y){
            Graphics2D p=(Graphics2D)g.create();p.setColor(palette.color());p.fillRoundRect(x+1,y+1,16,16,5,5);
            p.setColor(new Color(140,150,165));p.drawRoundRect(x+1,y+1,16,16,5,5);p.dispose();
          }
        });return label;
      }
    });return box;
  }
  static void show(ModernWorkspace owner){
    JComboBox<Palette> themes=choices(THEMES,theme),selections=choices(COLORS,selection),signals=choices(COLORS,signal);
    JPanel fields=new JPanel(new GridLayout(0,2,12,12));fields.setBorder(new EmptyBorder(8,8,8,8));
    JComboBox<String> languages=new JComboBox<>(new String[]{"English","Português (Brasil)"});
    languages.setSelectedIndex(I18n.language.equals("pt-BR")?1:0);
    languages.getAccessibleContext().setAccessibleName(I18n.t("Idioma"));
    fields.add(I18n.label(I18n.t("Idioma")));fields.add(languages);
    fields.add(I18n.label(I18n.t("Tema da área de trabalho")));fields.add(themes);
    fields.add(I18n.label(I18n.t("Cor da seleção")));fields.add(selections);
    fields.add(I18n.label(I18n.t("Cor do sinal 1")));fields.add(signals);
    JSlider alpha=new JSlider(85,100,Math.round(opacity*100));
    alpha.setMajorTickSpacing(5);alpha.setPaintTicks(true);alpha.setPaintLabels(true);
    alpha.setToolTipText(I18n.t("85% é mais translúcido; 100% é opaco."));
    fields.add(I18n.label(I18n.t("Opacidade dos painéis (%)")));fields.add(alpha);
    JCheckBox fade=I18n.check(I18n.t("Transições suaves ao abrir e fechar"),animations);
    fields.add(I18n.label(I18n.t("Animações")));fields.add(fade);
    themes.getAccessibleContext().setAccessibleName(I18n.t("Tema da área de trabalho"));
    selections.getAccessibleContext().setAccessibleName(I18n.t("Cor da seleção"));
    signals.getAccessibleContext().setAccessibleName(I18n.t("Cor do sinal 1"));
    alpha.getAccessibleContext().setAccessibleName(I18n.t("Opacidade dos painéis"));
    if(JOptionPane.showConfirmDialog(owner.frame,fields,I18n.t("Temas e aparência"),
      JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
      I18n.language=languages.getSelectedIndex()==1?"pt-BR":"en";
      theme=themes.getSelectedIndex();selection=selections.getSelectedIndex();signal=signals.getSelectedIndex();
      opacity=alpha.getValue()/100f;animations=fade.isSelected();apply();owner.hint(I18n.t("Aparência salva."));
    }
  }
}
