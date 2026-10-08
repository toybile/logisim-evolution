package local.logisim.panels;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.circuit.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.data.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

final class SimulationPanel extends JPanel {
  final Project project;
  private final Runnable changed;
  private final JPanel signals=new JPanel();
  private final JPanel inputSignals=new JPanel();
  private final JPanel outputSignals=new JPanel();
  private final List<Signal> rows=new ArrayList<>();
  private Circuit listedCircuit;
  private Set<Instance> listedPins=new HashSet<>();
  private final JLabel message=new JLabel();
  private final JCheckBox clocks=I18n.check(I18n.t("Relógio automático"));
  private boolean refreshing;

  SimulationPanel(Project project,Runnable changed){
    super(new BorderLayout(0,8));this.project=project;this.changed=changed;
    setBackground(Color.WHITE);
    signals.setLayout(new GridLayout(1,2,12,0));signals.setBackground(Color.WHITE);
    inputSignals.setLayout(new BoxLayout(inputSignals,BoxLayout.Y_AXIS));inputSignals.setOpaque(false);
    outputSignals.setLayout(new BoxLayout(outputSignals,BoxLayout.Y_AXIS));outputSignals.setOpaque(false);
    signals.add(inputSignals);signals.add(outputSignals);
    outputSignals.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,1,0,0,new Color(222,229,240)),BorderFactory.createEmptyBorder(0,12,0,0)));
    JScrollPane scroll=new JScrollPane(signals);scroll.setBorder(BorderFactory.createEmptyBorder());
    add(scroll,BorderLayout.CENTER);
    JPanel bottom=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));bottom.setOpaque(false);
    message.setFont(UiFonts.font("Segoe UI",Font.PLAIN,11));message.setForeground(new Color(99,111,129));
    message.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));message.setOpaque(true);message.setBackground(new Color(229,240,255));
    bottom.add(message);
    JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));controls.setOpaque(false);
    controls.add(ModernWorkspace.button(I18n.t("Executar"),I18n.t("Ativar propagação"),() -> project.getSimulator().setAutoPropagation(true)));
    controls.add(ModernWorkspace.button(I18n.t("Passo"),I18n.t("Propagar uma etapa"),() -> project.getSimulator().step()));
    controls.add(ModernWorkspace.button(I18n.t("Reiniciar"),I18n.t("Reiniciar valores"),() -> project.getSimulator().reset()));
    bottom.add(controls);
    clocks.setOpaque(false);
    clocks.addActionListener(e -> {
      if(!refreshing)project.getSimulator().setAutoTicking(clocks.isSelected());
    });
    clocks.setFont(UiFonts.font("Segoe UI",Font.PLAIN,10));bottom.add(clocks);add(bottom,BorderLayout.SOUTH);
    rebuildPins();
  }
  void rebuildPins(){
    Circuit circuit=project.getCurrentCircuit();
    Map<Instance,String> pins=circuit==null?Map.of():Analyze.getPinLabels(circuit);
    Set<Instance> set=new HashSet<>(pins.keySet());
    if(listedCircuit==circuit && listedPins.equals(set))return;
    listedCircuit=circuit;listedPins=set;inputSignals.removeAll();outputSignals.removeAll();rows.clear();
    if(pins.isEmpty()){
      JLabel empty=I18n.label(I18n.t("<html>Adicione pinos de entrada e saída<br>para controlar os sinais aqui.</html>"));
      empty.setForeground(new Color(107,118,135));inputSignals.add(empty);
    }else {
      var ordered=new ArrayList<>(pins.entrySet());
      ordered.sort(Comparator.comparing(entry -> !Pin.FACTORY.isInputPin(entry.getKey())));
      for(var entry:ordered){
        Instance pin=entry.getKey();
        String label=pin.getAttributeValue(StdAttr.LABEL);
        if(label==null || label.isBlank())label=entry.getValue();
        Signal row=new Signal(pin,label);rows.add(row);
        (Pin.FACTORY.isInputPin(pin)?inputSignals:outputSignals).add(row.panel);
      }
    }
    signals.revalidate();signals.repaint();refreshValues();
  }
  void invalidatePins(){listedCircuit=null;rebuildPins();}
  void refreshValues(){
    Circuit circuit=project.getCurrentCircuit();
    if(circuit!=listedCircuit || circuit!=null && !listedPins.equals(Analyze.getPinLabels(circuit).keySet())){
      rebuildPins();return;
    }
    refreshing=true;
    try{
      for(Signal row:rows){
        Value value=Pin.FACTORY.getValue(project.getCircuitState().getInstanceState(row.pin));
        if(row.input!=null&&!row.input.hasFocus()&&!row.input.getText().equals(value.toBinaryString()))row.input.setText(value.toBinaryString());
        if(value.equals(row.lastValue))continue;
        row.lastValue=value;
        row.value.setText(value.toDisplayString());
        if(row.toggle!=null){
          row.toggle.setSelected(Value.TRUE.equals(value));
          row.toggle.repaint();
        }
        if(row.input!=null && !row.input.hasFocus())row.input.setText(value.toBinaryString());
        row.value.setForeground(value.isErrorValue()?new Color(184,44,44):
            value.isFullyDefined()?new Color(43,100,59):new Color(117,112,84));
        row.panel.repaint();
      }
      clocks.setSelected(project.getSimulator().isAutoTicking());
      clocks.setEnabled(circuit!=null && !circuit.getClocks().isEmpty());
      message.setText(project.getSimulator().isAutoPropagating()?
          I18n.t("Ativa"):I18n.t("Pausada"));
      message.setToolTipText(project.getSimulator().isAutoPropagating()?I18n.t("Altere as entradas para testar."):I18n.t("Use Passo ou Executar para propagar."));
    }finally{refreshing=false;}
  }
  void dispose(){}
  private final class Signal {
    Value lastValue;
    final Instance pin;final JPanel panel=new JPanel(new BorderLayout(8,0));
    final JLabel value=new JLabel();
    final JToggleButton toggle;
    final JTextField input;
    Signal(Instance pin,String name){
      this.pin=pin;boolean isInput=Pin.FACTORY.isInputPin(pin);int width=Pin.FACTORY.getWidth(pin).getWidth();
      panel.setOpaque(false);panel.setBorder(BorderFactory.createEmptyBorder(isInput?2:4,0,isInput?2:4,0));
      panel.setPreferredSize(new Dimension(150,isInput?32:65));
      panel.setMinimumSize(new Dimension(80,isInput?32:65));
      panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,isInput?32:65));
      JLabel label=new JLabel(name+(width>1?" · "+width+" bits":""));
      label.setToolTipText(isInput?I18n.t("Entrada"):I18n.t("Saída"));
      label.setFont(UiFonts.font("Segoe UI",Font.PLAIN,12));panel.add(label,BorderLayout.WEST);
      value.setFont(UiFonts.font("Consolas",Font.BOLD,14));
      if(isInput && width==1){
        input=null;toggle=new JToggleButton(){
          protected void paintComponent(Graphics graphics){
            Graphics2D p=(Graphics2D)graphics.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int top=(getHeight()-22)/2;
            p.setColor(isSelected()?Appearance.signalOne():new Color(150,160,176));p.fillRoundRect(2,top,40,22,22,22);
            p.setColor(Color.WHITE);p.fillOval(isSelected()?24:5,top+3,16,16);
            p.setColor(new Color(31,45,64));p.setFont(UiFonts.font("Segoe UI",Font.PLAIN,13));p.drawString(isSelected()?"1":"0",51,top+16);
            if(isFocusOwner()){p.setColor(Appearance.accent());p.drawRoundRect(0,1,getWidth()-2,getHeight()-3,10,10);}p.dispose();
          }
        };
        toggle.setBorderPainted(false);toggle.setPreferredSize(new Dimension(70,28));
        I18n.tip(toggle,I18n.t("Alternar o valor de ")+name);
        toggle.addActionListener(e -> {if(!refreshing)drive(toggle.isSelected()?Value.TRUE:Value.FALSE);});
        panel.add(toggle,BorderLayout.EAST);
      }else if(isInput){
        toggle=null;input=new JTextField(width<=16?width:16);
        input.setToolTipText(I18n.t("Valor binário de ")+name+I18n.t("; pressione Enter para aplicar"));
        input.setFont(UiFonts.font("Consolas",Font.PLAIN,12));
        input.addActionListener(e -> {
          String text=input.getText().trim().replace("_","");
          if(!text.matches("[01]{1,"+width+"}")){
            input.setBorder(BorderFactory.createLineBorder(new Color(188,58,58)));
            I18n.tip(input,I18n.t("Use de 1 a ")+width+I18n.t(" dígitos binários (0 e 1)."));
            return;
          }
          long bits=Long.parseUnsignedLong(text,2);
          drive(Value.createKnown(Pin.FACTORY.getWidth(pin),bits));
          input.setBorder(UIManager.getBorder("TextField.border"));
        });
        panel.add(input,BorderLayout.EAST);
      }else {
        toggle=null;input=null;I18n.tip(value,I18n.t("Valor atual da saída ")+name);
        if(width==1){
          JPanel circle=new JPanel(){protected void paintComponent(Graphics graphics){
            Graphics2D p=(Graphics2D)graphics.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(new Color(36,49,67));p.setStroke(new BasicStroke(2f));p.drawOval(3,3,39,39);
            p.setFont(UiFonts.font("Segoe UI",Font.BOLD,22));p.drawString(value.getText(),16,31);p.dispose();
          }};circle.setOpaque(false);circle.setPreferredSize(new Dimension(48,48));panel.add(circle,BorderLayout.CENTER);
        }else panel.add(value,BorderLayout.EAST);
      }
    }
    void drive(Value value){
      if(project.getCurrentCircuit()!=listedCircuit)return;
      Pin.FACTORY.driveInputPin(project.getCircuitState().getInstanceState(pin),value);
      project.getCircuitState().markComponentAsDirty(pin.getComponent());
      project.getSimulator().nudge();project.repaintCanvas();refreshValues();changed.run();
    }
  }
}
