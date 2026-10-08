package local.logisim.panels;

import com.cburch.logisim.Main;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.util.LocaleManager;
import com.formdev.flatlaf.FlatLaf;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public final class Launcher {
  static Path home() {
    String override=System.getProperty("logisim.panels.home");
    if(override!=null&&!override.isBlank())return Paths.get(override).toAbsolutePath();
    String base=System.getenv(System.getProperty("os.name").startsWith("Windows")?"LOCALAPPDATA":"XDG_CONFIG_HOME");
    if(base==null||base.isBlank())base=Paths.get(System.getProperty("user.home"),".config").toString();
    return Paths.get(base,"logisim-panels");
  }

  public static void main(String[] args) throws Exception {
    Files.createDirectories(home());
    System.setErr(new PrintStream(Files.newOutputStream(home().resolve("diagnostico.log"),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND), true, "UTF-8"));
    FlatLaf.registerCustomDefaultsSource("local.logisim.panels.theme");
    FlatLaf.setPreferredFontFamily(UiFonts.preferred());
    FlatLaf.setPreferredMonospacedFontFamily("Consolas");
    Appearance.load(home().resolve("aparencia.properties"));
    Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
      if (event instanceof WindowEvent e && e.getID() == WindowEvent.WINDOW_OPENED
          && e.getWindow() instanceof Frame frame) {
        SwingUtilities.invokeLater(() -> {
          if (frame.getRootPane().getClientProperty("floating.workspace") != null) return;
          try {
            LocaleManager.setLocale(I18n.locale());
            ModernWorkspace workspace=ModernWorkspace.install(frame, home().resolve("disposicao.properties"));
            javax.swing.Timer center=new javax.swing.Timer(300,ignored -> workspace.fitCircuit());
            center.setRepeats(false);center.start();
          } catch (Throwable failure) {
            failure.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                I18n.t("A nova interface não pôde ser carregada. O editor original continua disponível.\n")
                + failure.getMessage(), "Logisim", JOptionPane.ERROR_MESSAGE);
          }
        });
      }
    }, AWTEvent.WINDOW_EVENT_MASK);
    LocaleManager.setLocale(I18n.locale());
    Main.main(args);
  }
}
