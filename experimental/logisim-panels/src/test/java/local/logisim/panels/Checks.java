package local.logisim.panels;

import com.cburch.logisim.file.*;
import com.cburch.logisim.proj.*;
import com.cburch.logisim.circuit.*;
import com.cburch.logisim.comp.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.data.*;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.tools.*;
import com.cburch.logisim.util.LocaleManager;
import com.formdev.flatlaf.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Integration checks use generated circuits, never the user's open project. */
public final class Checks {
  private static int checks;
  static void require(boolean condition,String message){
    checks++;if(!condition)throw new AssertionError(message);System.out.println("OK "+message);
  }
  static com.cburch.logisim.comp.Component gate(LogisimFile file,String name,int x,int y){
    Tool tool=ModernWorkspace.findTool(file,name,new HashSet<>());
    if(!(tool instanceof AddTool add))throw new IllegalArgumentException(name);
    var attrs=add.getFactory().createAttributeSet();
    for(Attribute attr:attrs.getAttributes())if(attr.getName().equals("inputs"))attrs.setValue(attr,2);
    return add.getFactory().createComponent(Location.create(x,y,true),attrs);
  }
  static com.cburch.logisim.comp.Component pin(String name,boolean input,int x,int y){
    var attrs=Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL,name);attrs.setValue(Pin.ATTR_TYPE,input?Pin.INPUT:Pin.OUTPUT);
    attrs.setValue(StdAttr.FACING,input?Direction.EAST:Direction.WEST);
    return Pin.FACTORY.createComponent(Location.create(x,y,true),attrs);
  }
  static void route(CircuitMutation m,Location a,Location b,int bend){
    Location p=Location.create(bend,a.getY(),true),q=Location.create(bend,b.getY(),true);
    if(!a.equals(p))m.add(Wire.create(a,p));if(!p.equals(q))m.add(Wire.create(p,q));
    if(!q.equals(b))m.add(Wire.create(q,b));
  }
  static Project demo() {
    Loader loader=new Loader(null);
    LogisimFile file=LogisimFile.createNew(loader,null);
    for(Library library:loader.getBuiltin().getLibraries())file.addLibrary(library);
    for(String name:new String[]{"Poke Tool","Edit Tool","Wiring Tool","Text Tool","Pin","AND Gate","OR Gate","NOT Gate"}){
      Tool tool=ModernWorkspace.findTool(file,name,new HashSet<>());
      if(tool!=null)file.getOptions().getToolbarData().addTool(tool.cloneTool());
    }
    Project project=new Project(file);project.getSimulator().setAutoPropagation(false);
    Circuit circuit=project.getCurrentCircuit();circuit.setName("Exemplo");
    var a=pin("A",true,320,140);var b=pin("B",true,320,220);var c=pin("C",true,320,320);
    var y=pin("Y",false,840,230);
    var and=gate(file,"AND Gate",520,180);var not=gate(file,"NOT Gate",520,320);
    var or=gate(file,"OR Gate",720,230);
    CircuitMutation mutation=new CircuitMutation(circuit);
    mutation.addAll(java.util.List.of(a,b,c,y,and,not,or));
    Instance ai=Instance.getInstanceFor(and),ni=Instance.getInstanceFor(not),oi=Instance.getInstanceFor(or);
    route(mutation,a.getLocation(),ai.getPortLocation(1),380);
    route(mutation,b.getLocation(),ai.getPortLocation(2),400);
    route(mutation,c.getLocation(),ni.getPortLocation(1),450);
    route(mutation,ai.getPortLocation(0),oi.getPortLocation(1),610);
    route(mutation,ni.getPortLocation(0),oi.getPortLocation(2),630);
    route(mutation,oi.getPortLocation(0),y.getLocation(),800);
    mutation.execute();project.setFileAsClean();return project;
  }
  public static void main(String[] args) throws Exception {
    FlatLaf.registerCustomDefaultsSource("local.logisim.panels.theme");FlatLightLaf.setup();
    LocaleManager.setLocale(Locale.forLanguageTag("pt"));
    Project project=demo();
    Path folder=Paths.get(args.length>0?args[0]:".").toAbsolutePath();
    Path fixture=folder.resolve("Exemplo.circ");
    try(OutputStream out=Files.newOutputStream(fixture)){project.getLogisimFile().write(out,
        project.getLogisimFile().getLoader(),fixture.toFile());}
    Project loaded=new Project(new Loader(null).openLogisimFile(fixture.toFile()));
    loaded.getSimulator().setAutoPropagation(false);
    Circuit circuit=loaded.getCurrentCircuit();
    TruthEngine.Result truth=TruthEngine.calculate(loaded,circuit);
    require(truth.rows().length==8,"analisador calcula oito combinações");
    int[] expected={1,0,1,0,1,0,1,1};
    require(Arrays.equals(truth.headers(),new String[]{"A","B","C","Y"}),"rótulos preservados ao salvar e abrir");
    for(int row=0;row<8;row++)
      require(truth.rows()[row][3].equals(""+expected[row]),"saída correta da tabela na combinação "+row);
    String stateBefore=loaded.getCircuitState().getValue(Location.create(840,230,true)).toDisplayString();
    TruthEngine.calculate(loaded,circuit);
    require(stateBefore.equals(loaded.getCircuitState().getValue(Location.create(840,230,true)).toDisplayString()),
        "análise preserva o estado do circuito em edição");
    var pins=Analyze.getPinLabels(circuit);
    CircuitState isolated=CircuitState.createRootState(loaded,circuit,Thread.currentThread());
    for(int row=0;row<8;row++){
      for(var entry:pins.entrySet()){
        Instance pin=entry.getKey();String name=entry.getValue();
        if(Pin.FACTORY.isInputPin(pin)){
          int mask=name.equals("A")?4:name.equals("B")?2:1;
          Pin.FACTORY.driveInputPin(isolated.getInstanceState(pin),(row&mask)!=0?Value.TRUE:Value.FALSE);
          isolated.markComponentAsDirty(pin.getComponent());
        }
      }
      isolated.getPropagator().propagate();
      require(isolated.getValue(Location.create(840,230,true)).toLongValue()==expected[row],
          "simulador propaga as entradas na combinação "+row);
    }
    final ModernWorkspace[] workspace=new ModernWorkspace[1];
    final Frame[] nativeFrame=new Frame[1];
    SwingUtilities.invokeAndWait(() -> {
      try {
        Frame frame=new Frame(loaded);nativeFrame[0]=frame;frame.setSize(1280,850);
        ModernWorkspace ui=ModernWorkspace.install(frame,folder.resolve("check-disposicao.properties"));
        workspace[0]=ui;
        ui.surface.setSize(1200,680);ui.arrangeSurface();
        require(SwingUtilities.isDescendingFrom(frame.getCanvas(),ui.nativeView),"área de desenho original integrada");
        require(SwingUtilities.isDescendingFrom((JComponent)ModernWorkspace.field(frame,"toolbox"),ui),
            "biblioteca completa integrada");
        require(SwingUtilities.isDescendingFrom((JComponent)ModernWorkspace.field(frame,"attrTable"),ui),
            "propriedades editáveis originais integradas");
        FloatingPanel panel=ui.panels.get("properties");
        ui.show("properties");panel.setBounds(442,64,330,370);ui.constrain(panel);
        panel.setMinimized(true);int expanded=panel.expandedHeight;
        ui.saveLayout();
        Properties p=new Properties();try(InputStream in=Files.newInputStream(folder.resolve("check-disposicao.properties"))){p.load(in);}
        ui.defaultLayout();ui.restoreLayout(p);
        require(panel.getX()==442&&panel.getY()==64,"posição personalizada restaurada");
        require(panel.minimized&&panel.expandedHeight==expanded,"minimização e tamanho expandido restaurados");
        panel.setMinimized(false);require(panel.getHeight()==370,"expansão restaura tamanho anterior");
        ui.hide("properties");require(!panel.isVisible(),"fechar oculta sem destruir controles");
        ui.show("properties");require(panel.isVisible(),"painel pode reabrir");
        panel.setBounds(5000,-400,900,1000);ui.constrain(panel);
        require(panel.getX()>=0&&panel.getY()>=0&&panel.getX()+panel.getWidth()<=ui.surface.getWidth()
            &&panel.getY()+panel.getHeight()<=ui.surface.getHeight(),"painel permanece acessível após resize");
        ui.surface.setSize(650,380);ui.arrangeSurface();
        for(FloatingPanel floating:ui.panels.values())
          require(floating.getX()+floating.getWidth()<=650&&floating.getY()+floating.getHeight()<=380,
              "painel "+floating.id+" cabe em janela menor");
        ui.restoreOriginal();
        require(frame.getContentPane()==ui.original,"recuperação do editor original preserva a janela");
        require(SwingUtilities.isDescendingFrom((JComponent)ModernWorkspace.field(frame,"toolbox"),frame),
            "biblioteca retorna ao editor original");
        LocaleManager.setLocale(Locale.ENGLISH);LocaleManager.setLocale(Locale.forLanguageTag("pt"));
        require(true,"troca de idioma preserva os controles originais");
        frame.dispose();
      }catch(Exception e){throw new RuntimeException(e);}
    });
    loaded.getSimulator().shutDown();project.getSimulator().shutDown();
    loaded.getLogisimFile().stopAutosaveThread(false);project.getLogisimFile().stopAutosaveThread(false);
    System.out.println("PASSOU: "+checks+" verificações de integração");
    System.exit(0);
  }
}
