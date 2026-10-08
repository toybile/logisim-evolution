package local.logisim.panels;

import java.awt.*;
import java.io.*;
import java.util.*;

public final class UiFonts {
  private static final Set<String> installed=new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
  private static final Font bundled=load();
  private static Font load(){
    try(var in=UiFonts.class.getResourceAsStream("/fonts/Nunito.ttf")){
      Font font=Font.createFont(Font.TRUETYPE_FONT,in);GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);return font;
    }catch(Exception ignored){return new Font(Font.DIALOG,Font.PLAIN,12);}
  }
  public static Font font(String family,int style,int size){
    if(installed.contains(family))return new Font(family,style,size);
    if(family.equals("Consolas"))return new Font(Font.MONOSPACED,style,size);
    if(family.equals("Segoe UI Symbol"))return new Font(Font.DIALOG,style,size);
    return bundled.deriveFont(style,(float)size);
  }
  static String preferred(){return installed.contains("Segoe UI")?"Segoe UI":bundled.getFamily();}
}
