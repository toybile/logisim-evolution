package local.logisim.panels;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.*;
import com.cburch.logisim.instance.StdAttr;
import com.formdev.flatlaf.FlatLightLaf;
import com.cburch.logisim.util.LocaleManager;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

public final class UxChecks {
  static int checks;
  static void require(boolean b,String message){checks++;if(!b)throw new AssertionError(message);System.out.println("OK "+message);}
  static int toolCount(Library l,Set<Library> seen){if(!seen.add(l))return 0;int n=l.getTools().size();for(Library child:l.getLibraries())n+=toolCount(child,seen);return n;}
  static void drag(JComponent target,int x,int y,int dx,int dy){
    long now=System.currentTimeMillis();
    target.dispatchEvent(new MouseEvent(target,MouseEvent.MOUSE_PRESSED,now,InputEvent.BUTTON1_DOWN_MASK,x,y,100+x,100+y,1,false,MouseEvent.BUTTON1));
    target.dispatchEvent(new MouseEvent(target,MouseEvent.MOUSE_DRAGGED,now+20,InputEvent.BUTTON1_DOWN_MASK,x+dx,y+dy,100+x+dx,100+y+dy,0,false,MouseEvent.NOBUTTON));
    target.dispatchEvent(new MouseEvent(target,MouseEvent.MOUSE_RELEASED,now+40,0,x+dx,y+dy,100+x+dx,100+y+dy,1,false,MouseEvent.BUTTON1));
  }
  public static void main(String[] args)throws Exception{
    Thread.setDefaultUncaughtExceptionHandler((thread,error) -> {error.printStackTrace();System.exit(1);});
    I18n.language="pt-BR";FlatLightLaf.setup();LocaleManager.setLocale(Locale.forLanguageTag("pt"));Project p=Checks.demo();LocaleManager.setLocale(I18n.locale());
    SwingUtilities.invokeAndWait(() -> {
      try{
        Frame frame=new Frame(p);frame.setSize(1600,1000);
        ModernWorkspace ui=ModernWorkspace.install(frame,Paths.get(args[0]).resolve("check-ux.properties"));
        frame.setVisible(true);ui.surface.setSize(1600,950);ui.arrangeSurface();
        for(String id:ui.panels.keySet()){
          ui.show(id);require(ui.toggles.get(id).isSelected(),"símbolo "+id+" indica painel aberto");
          ui.hide(id);require(!ui.toggles.get(id).isSelected(),"símbolo "+id+" acompanha fechamento");ui.show(id);
        }
        require(ui.toolbar.buttons.size()==15,"todos os quinze símbolos permanecem disponíveis");
        require(ui.toolbar.buttons.stream().allMatch(b -> b.getText()==null||b.getText().isEmpty()),"barra contém somente símbolos");
        require(ui.library.accessibleTools.size()==toolCount(p.getLogisimFile(),new HashSet<>()),"categorias cobrem todas as ferramentas carregadas");
        require(ui.library.managementMenu().getComponentCount()>=8,"menu mantém os seis comandos nativos e a lista completa");
        ui.library.search.setText("AND");require(ui.library.categories.getComponentCount()>1,"pesquisa encontra componentes sem remover bibliotecas");ui.library.search.setText("");
        ui.toolbar.editButtons.get("Edit Tool").doClick();require(p.getTool().getName().equals("Edit Tool"),"símbolo seleciona ferramenta real de edição");
        ui.toolbar.editButtons.get("Poke Tool").doClick();require(p.getTool().getName().equals("Poke Tool"),"símbolo seleciona ferramenta real de interação");
        require(ui.toolbar.nativeToolsMenu().getComponentCount()>=8,"ferramentas adicionais mantêm os atalhos nativos");
        ui.toolbar.setBounds(400,40,690,72);ui.toolbar.ensureCapacity();require(ui.toolbar.columnCount()==15,"barra estendida organiza símbolos em uma linha");
        ui.toolbar.setBounds(400,40,74,704);ui.toolbar.ensureCapacity();require(ui.toolbar.columnCount()==1,"barra estreita organiza símbolos em uma coluna");
        for(AbstractButton b:ui.toolbar.buttons)require(b.getY()+b.getHeight()<=ui.toolbar.getHeight(),"símbolo cabe na barra vertical");
        Properties saved=ui.captureLayout();ui.toolbar.setBounds(500,30,690,72);ui.restoreLayout(saved);
        require(ui.toolbar.getX()==400&&ui.toolbar.getWidth()==74,"posição e tamanho da barra são restaurados");
        FloatingPanel panel=ui.panels.get("properties");
        int[][] edges={{1,150,-35,0},{419,150,35,0},{200,1,0,-35},{200,379,0,35},{1,1,-35,-35},{419,1,35,-35},{1,379,-35,35},{419,379,35,35}};
        for(int i=0;i<edges.length;i++){
          panel.setBounds(600,300,420,380);int[] e=edges[i];drag(panel,e[0],e[1],e[2],e[3]);Rectangle b=panel.getBounds();
          require(b.width==(e[2]==0?420:455)&&b.height==(e[3]==0?380:415),"arraste real redimensiona pela direção "+i);
          if(e[2]<0)require(b.x+b.width==1020,"borda esquerda preserva a direita");
          if(e[3]<0)require(b.y+b.height==680,"borda superior preserva a inferior");
        }
        panel.setBounds(600,300,420,380);
        JPanel footer=(JPanel)((BorderLayout)panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        JComponent corner=(JComponent)footer.getComponent(0);drag(corner,4,4,35,25);
        require(panel.getWidth()==455&&panel.getHeight()==405,"controle do canto continua ajustando largura e altura juntas");
        ui.toolbar.setBounds(400,40,690,72);drag(ui.toolbar,689,30,-450,0);
        require(ui.toolbar.columnCount()>1&&ui.toolbar.columnCount()<15,"arraste da borda reorganiza a barra em várias linhas");
        panel.setBounds(10,10,420,380);drag(panel,1,1,-10000,-10000);require(panel.getX()==8&&panel.getY()==8,"resize respeita limites da área do circuito");
        ui.surface.setSize(650,380);ui.arrangeSurface();
        require(ui.toolbar.getX()+ui.toolbar.getWidth()<=650&&ui.toolbar.getY()+ui.toolbar.getHeight()<=380,"barra permanece acessível em janela pequena");
        for(AbstractButton b:ui.toolbar.buttons)require(b.getY()+b.getHeight()<=ui.toolbar.getHeight(),"ícones permanecem acessíveis ao reduzir janela");
        ui.surface.setSize(1600,950);ui.arrangeSurface();
        var gate=p.getCurrentCircuit().getNonWires().stream().filter(c -> c.getFactory().getName().equals("AND Gate")).findFirst().orElseThrow();
        p.getSelection().add(gate);frame.viewComponentAttributes(p.getCurrentCircuit(),gate);ui.propertyForm.refresh();
        require(!ui.propertyForm.bindings.isEmpty(),"propriedades mostram campos ligados ao modelo nativo");
        var label=ui.propertyForm.bindings.stream().filter(b -> b.row().getLabel().equals(StdAttr.LABEL.getDisplayName())).findFirst().orElseThrow();
        ui.propertyForm.commit(label.row(),"AND1");require(gate.getAttributeSet().getValue(StdAttr.LABEL).equals("AND1"),"campo altera atributo real do componente");
        p.undoAction();require(!gate.getAttributeSet().getValue(StdAttr.LABEL).equals("AND1"),"edição pelo formulário mantém desfazer nativo");
        ui.restoreOriginal();require(frame.getContentPane()==ui.original,"editor original permanece recuperável após reorganização");frame.setVisible(false);
      }catch(Exception e){throw new RuntimeException(e);}
    });
    p.getSimulator().shutDown();p.getLogisimFile().stopAutosaveThread(false);System.out.println("PASSOU: "+checks+" verificações de interação");System.exit(0);
  }
}
