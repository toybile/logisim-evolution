package local.logisim.panels;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.util.LocaleManager;
import com.formdev.flatlaf.FlatLightLaf;

public final class LanguageChecks {
  static int checks;
  static void require(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);System.out.println("OK "+text);}
  static String caption(FloatingPanel panel){return ((JLabel)((BorderLayout)panel.header.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getText();}
  static void chooseLanguage(ModernWorkspace ui,int index){
    // Operate the real settings dialog rather than directly changing the language variable.
    final boolean[] accepted={false};final long deadline=System.nanoTime()+5_000_000_000L;
    javax.swing.Timer choose=new javax.swing.Timer(80,null);
    choose.addActionListener(event->{
      for(Window window:ui.frame.getOwnedWindows()){
        if(!window.isShowing())continue;
        JOptionPane pane=LibraryPanel.find(window,JOptionPane.class);if(pane==null)continue;
        JComboBox<?> language=LibraryPanel.find(pane,JComboBox.class);if(language==null)continue;
        if(language.getItemCount()!=2||!"English".equals(language.getItemAt(0)))throw new AssertionError("Missing language control");
        language.setSelectedIndex(index);
        // JOptionPane's public value models an actual accepted settings form.
        pane.setValue(JOptionPane.OK_OPTION);choose.stop();accepted[0]=true;return;
      }
      if(System.nanoTime()>deadline){choose.stop();throw new AssertionError("Settings dialog did not appear");}
    });
    choose.start();Appearance.show(ui);if(!accepted[0])throw new AssertionError("Settings were not accepted");
  }
  public static void main(String[] args)throws Exception{
    Thread.setDefaultUncaughtExceptionHandler((thread,error)->{error.printStackTrace();System.exit(1);});
    FlatLightLaf.setup();Path settings=Paths.get(args[0],"languages.properties");Files.deleteIfExists(settings);
    Appearance.load(settings);require(I18n.language.equals("en"),"English is the default with no saved settings");
    LocaleManager.setLocale(I18n.locale());var project=Checks.demo();
    SwingUtilities.invokeAndWait(()->{
      try{
        Frame frame=new Frame(project);frame.setSize(1600,1000);
        ModernWorkspace ui=ModernWorkspace.install(frame,Paths.get(args[0],"language-layout.properties"));
        frame.setVisible(true);ui.arrangeSurface();
        require(caption(ui.panels.get("library")).equals("Library"),"panel starts in English");
        require(ui.library.quickTitle.getText().equals("Quick access"),"quick access starts in English");
        require(ui.library.tabs.getTitleAt(0).equals("Components"),"library tab starts in English");
        require("Search components…".equals(ui.library.search.getClientProperty("JTextField.placeholderText")),"search starts in English");
        Properties layout=ui.captureLayout();ui.library.quickTitle.doClick();
        chooseLanguage(ui,1);
        require(caption(ui.panels.get("library")).equals("Biblioteca"),"panel changes to Portuguese immediately");
        require(ui.library.quickTitle.getText().equals("Acesso rápido"),"quick access changes language");
        require(!ui.library.favorites.isVisible(),"language change preserves collapsed quick access");
        require(ui.library.tabs.getTitleAt(0).equals("Componentes"),"tab changes to Portuguese");
        require(ui.toggles.get("simulation").getToolTipText().contains("Simulação"),"symbol shortcut tooltip changes language");
        require(ui.captureLayout().equals(layout),"language change preserves panel positions, sizes, and visibility");
        Appearance.load(settings);require(I18n.language.equals("pt-BR"),"Portuguese selection is persisted");
        chooseLanguage(ui,0);
        require(caption(ui.panels.get("properties")).equals("Properties"),"second switch restores English");
        require(ui.library.tabs.getTitleAt(0).equals("Components"),"tab returns to English");
        require(ui.toolbar.buttons.get(0).getToolTipText().equals("Select and move components"),"editing tooltip returns to English");
        require(Arrays.stream(ui.nativeMenu.getComponents()).anyMatch(c->c instanceof JMenu m&&m.getText().equals("File")),"native Logisim menus switch to English too");
        Appearance.load(settings);require(I18n.language.equals("en"),"English selection is persisted");
        require(ui.zoomOverlay instanceof RoundedOverlay&&RoundedOverlay.ARC>18,"zoom overlay has stronger rounding than panels");
        var gate=project.getCurrentCircuit().getNonWires().stream().filter(c->c.getFactory().getName().equals("AND Gate")).findFirst().orElseThrow();
        project.getSelection().add(gate);frame.viewComponentAttributes(project.getCurrentCircuit(),gate);ui.propertyForm.refresh();
        require(ui.propertyForm.bindings.stream().anyMatch(b->b.row().getLabel().equals("Label")),"English native label attribute remains editable");
        require(UiFonts.font("Segoe UI",Font.PLAIN,12).canDisplayUpTo("English Português (Brasil)")==-1,"UI font supports both languages");
        ui.zoomOverlay.doLayout();
        var preview=new java.awt.image.BufferedImage(ui.zoomOverlay.getWidth()+24,ui.zoomOverlay.getHeight()+24,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics=preview.createGraphics();graphics.setColor(Appearance.canvas());graphics.fillRect(0,0,preview.getWidth(),preview.getHeight());graphics.translate(12,12);ui.zoomOverlay.paint(graphics);graphics.dispose();
        javax.imageio.ImageIO.write(preview,"png",Paths.get(args[0],"zoom-rounded.png").toFile());
        ui.dispose();frame.dispose();
      }catch(Exception e){throw new RuntimeException(e);}
    });
    project.getSimulator().shutDown();project.getLogisimFile().stopAutosaveThread(false);
    System.out.println("PASSOU: "+checks+" language checks");System.exit(0);
  }
}
