package local.logisim.panels;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.data.Value;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

public final class AppearanceChecks {
  static int checks;
  static void require(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);System.out.println("OK "+text);}
  static int alpha(FloatingPanel panel){
    BufferedImage image=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_ARGB);
    Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();return image.getRGB(20,100)>>>24;
  }
  public static void main(String[] args)throws Exception{
    Thread.setDefaultUncaughtExceptionHandler((thread,error)->{error.printStackTrace();System.exit(1);});
    FlatLightLaf.setup();Appearance.load(Paths.get(args[0],"check-appearance.properties"));
    var p=Checks.demo();var result=TruthEngine.calculate(p,p.getCurrentCircuit());
    require(result.equation().contains("Y =")&&result.equation().contains("∧")&&result.equation().contains("∨"),"equação real usa a notação proposicional");
    require(UiFonts.font("Segoe UI Symbol",Font.BOLD,14).canDisplayUpTo(result.equation())==-1,"fonte mostra todos os símbolos da equação");
    var assignmentClass=Class.forName("com.cburch.logisim.analyze.model.Assignments");
    var constructor=assignmentClass.getDeclaredConstructor();constructor.setAccessible(true);
    var put=assignmentClass.getDeclaredMethod("put",String.class,boolean.class);put.setAccessible(true);
    var evaluate=Expression.class.getMethod("evaluate",assignmentClass);
    var expression=result.model().getOutputExpressions().getExpression("Y");
    for(int row=0;row<8;row++){
      var assignments=constructor.newInstance();
      put.invoke(assignments,"A",(row&4)!=0);put.invoke(assignments,"B",(row&2)!=0);put.invoke(assignments,"C",(row&1)!=0);
      boolean value=(Boolean)evaluate.invoke(expression,assignments);
      require(value==result.rows()[row][3].equals("1"),"equação coincide com a tabela na combinação "+row);
    }
    CountDownLatch finished=new CountDownLatch(1);
    SwingUtilities.invokeAndWait(()->{
      try{
        Frame frame=new Frame(p);frame.setSize(1400,900);
        ModernWorkspace ui=ModernWorkspace.install(frame,Paths.get(args[0],"check-appearance-layout.properties"));
        frame.setVisible(true);ui.surface.setSize(1400,850);ui.arrangeSurface();
        Appearance.theme=2;Appearance.selection=2;Appearance.signal=5;Appearance.opacity=.94f;Appearance.animations=true;Appearance.apply();
        require(ui.frame.getCanvas().getBackground().equals(Appearance.THEMES[2].color()),"tema muda o fundo com ou sem grade");
        require(Value.TRUE.getColor().equals(Appearance.COLORS[5].color()),"cor do sinal 1 chega ao simulador nativo");
        require(UIManager.getColor("TabbedPane.underlineColor").equals(Appearance.COLORS[2].color()),"seleção usa cor independente");
        var underline=ui.library.tabs.getUI().getClass().getDeclaredField("underlineColor");underline.setAccessible(true);
        require(underline.get(ui.library.tabs.getUI()).equals(Appearance.accent()),"indicador real da aba atualiza a cor sem manter o delegado anterior");
        require(((JScrollPane)ui.simulation.getComponent(0)).getBorder() instanceof javax.swing.border.EmptyBorder,"trocar tema conserva a área de sinais sem contorno adicional");
        Appearance.load(Paths.get(args[0],"check-appearance.properties"));
        require(Appearance.theme==2&&Appearance.selection==2&&Appearance.signal==5,"cores são restauradas do arquivo");
        FloatingPanel panel=ui.panels.get("properties");panel.setBounds(700,20,380,560);panel.setVisible(true);
        int settled=alpha(panel);require(settled>=238&&settled<=242,"painel produz composição translúcida de 94 por cento");
        Timer begin=new Timer(450,beginEvent->{
        ui.hide("properties");
        require(!panel.isOpen()&&panel.isVisible()&&!ui.toggles.get("properties").isSelected(),"fechamento anima sem deixar símbolo selecionado");
        Timer middle=new Timer(55,e->{
          try{
            int fading=alpha(panel);require(fading>0&&fading<settled,"fade-out tem quadros intermediários");
            ui.show("properties");
            Timer end=new Timer(260,event->{
              try{
                require(panel.isOpen()&&panel.isVisible(),"reabrir durante fade-out cancela o fechamento anterior");
                require(alpha(panel)>=238,"fade-in termina na opacidade configurada");
                require(ui.toggles.get("properties").isSelected(),"símbolo acompanha painel reaberto");
                ui.hide("properties");
                Timer hidden=new Timer(240,hiddenEvent->{
                  try{
                    require(!panel.isVisible(),"fade-out termina ocultando o painel");
                    Appearance.animations=false;ui.show("properties");ui.hide("properties");
                    require(!panel.isVisible(),"desativar animações aplica o fechamento imediatamente");
                    ui.dispose();frame.setVisible(false);finished.countDown();
                  }catch(Throwable failure){failure.printStackTrace();System.exit(1);}
                });hidden.setRepeats(false);hidden.start();
              }catch(Throwable failure){failure.printStackTrace();System.exit(1);}
            });end.setRepeats(false);end.start();
          }catch(Throwable failure){failure.printStackTrace();System.exit(1);}
        });middle.setRepeats(false);middle.start();
        });begin.setRepeats(false);begin.start();
      }catch(Exception failure){throw new RuntimeException(failure);}
    });
    if(!finished.await(8,TimeUnit.SECONDS))throw new AssertionError("Transições não concluíram");
    System.out.println("PASSOU: "+checks+" verificações de aparência e equação");System.exit(0);
  }
}
