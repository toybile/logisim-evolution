package local.logisim.panels;

import com.cburch.logisim.proj.*;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.file.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.table.*;

final class TruthPanel extends JPanel {
  final Project project;
  final JFrame owner;
  final JTable table=new JTable();
  final JLabel equation=new JLabel("",SwingConstants.RIGHT);
  private final JLabel info=I18n.label(I18n.t("Calcule a tabela do circuito atual."));
  private final JButton calculate,export;
  private SwingWorker<TruthEngine.Result,Void> worker;
  private int generation;
  private boolean disposed;
  private TruthEngine.Result result;
  private int currentInputRow=-1;
  private final javax.swing.Timer recalculate=new javax.swing.Timer(650,e -> ensureCalculated());
  private final ProjectListener listener=e -> {
    if(e.getAction()==ProjectEvent.ACTION_COMPLETE || e.getAction()==ProjectEvent.UNDO_COMPLETE ||
        e.getAction()==ProjectEvent.REDO_COMPLETE || e.getAction()==ProjectEvent.ACTION_SET_CURRENT ||
        e.getAction()==ProjectEvent.ACTION_SET_FILE)SwingUtilities.invokeLater(this::invalidateResult);
  };
  TruthPanel(Project project,JFrame owner){
    super(new BorderLayout(0,7));this.project=project;this.owner=owner;
    setBackground(Color.WHITE);
    JPanel header=new JPanel(new BorderLayout(8,0));header.setOpaque(false);
    JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));actions.setOpaque(false);
    calculate=ModernWorkspace.button(I18n.t("Calcular"),I18n.t("Calcular todas as combinações do circuito atual"),this::calculate);
    export=ModernWorkspace.button(I18n.t("Exportar CSV"),I18n.t("Salvar os valores da tabela em CSV"),this::export);
    export.setEnabled(false);actions.add(calculate);actions.add(export);
    header.add(actions,BorderLayout.WEST);
    equation.setFont(UiFonts.font("Segoe UI Symbol",Font.BOLD,14));equation.setForeground(new Color(45,64,86));
    equation.getAccessibleContext().setAccessibleName(I18n.t("Equação proposicional do circuito"));
    header.add(equation,BorderLayout.CENTER);add(header,BorderLayout.NORTH);
    calculate.setIcon(new UiIcon("table",17));export.setIcon(new UiIcon("download",17));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setRowHeight(25);table.setShowGrid(true);
    table.setGridColor(new Color(231,236,245));table.setIntercellSpacing(new Dimension(1,1));
    table.getTableHeader().setBackground(new Color(240,244,250));
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
      public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
        super.getTableCellRendererComponent(t,v,s,f,r,c);setHorizontalAlignment(CENTER);
        if(!s)setBackground(r==currentInputRow?Appearance.selectionBackground():Color.WHITE);
        return this;
      }
    });
    JScrollPane scroll=new JScrollPane(table);scroll.setBorder(BorderFactory.createLineBorder(new Color(222,228,236)));
    scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter(){
      public void componentResized(java.awt.event.ComponentEvent e){
        fitColumns(scroll.getViewport().getWidth());
      }
    });
    add(scroll,BorderLayout.CENTER);info.setFont(UiFonts.font("Segoe UI",Font.PLAIN,11));
    I18n.tip(info,I18n.t("Tabela para circuitos combinacionais, até 10 bits de entrada."));
    add(info,BorderLayout.SOUTH);project.addProjectListener(listener);recalculate.setRepeats(false);
  }
  void invalidateResult(){
    if(disposed)return;generation++;result=null;currentInputRow=-1;export.setEnabled(false);
    equation.setText("");equation.setToolTipText(null);
    table.setModel(new DefaultTableModel());
    info.setText(I18n.t("Circuito alterado. Clique em Calcular para atualizar."));
    if(worker!=null){worker.cancel(false);worker=null;calculate.setEnabled(true);}
    if(isShowing())recalculate.restart();
  }
  void ensureCalculated(){if(result==null&&worker==null&&!disposed)calculate(false);}
  void calculate(){calculate(true);}
  private void calculate(boolean showErrors){
    if(worker!=null)return;
    final Circuit circuit=project.getCurrentCircuit();if(circuit==null)return;
    try {
      TruthEngine.checkCombinational(circuit,new java.util.HashSet<>());
      // Serialize on the UI thread, then analyze an isolated circuit off the UI thread.
      // Editing or simulator input changes cannot alter the truth-table calculation.
      ByteArrayOutputStream bytes=new ByteArrayOutputStream();
      project.getLogisimFile().write(bytes,project.getLogisimFile().getLoader());
      final byte[] snapshot=bytes.toByteArray();
      final String name=circuit.getName();final int request=++generation;
      calculate.setEnabled(false);export.setEnabled(false);info.setText(I18n.t("Calculando…"));
      worker=new SwingWorker<>(){
        Project copy;
        protected TruthEngine.Result doInBackground() throws Exception {
          try {
            Loader loader=new Loader(owner);
            LogisimFile file=LogisimFile.load(new ByteArrayInputStream(snapshot),loader);
            copy=new Project(file);copy.getSimulator().setAutoPropagation(false);
            return TruthEngine.calculate(copy,file.getCircuit(name));
          }finally{if(copy!=null){copy.getSimulator().shutDown();copy.getLogisimFile().stopAutosaveThread(false);}}
        }
        protected void done(){
          if(disposed||request!=generation)return;
          worker=null;calculate.setEnabled(true);
          try{
            result=get();
            equation.setText(result.equation());equation.setToolTipText(result.equation());
            table.setModel(new DefaultTableModel(result.rows(),result.headers()){
              public boolean isCellEditable(int row,int col){return false;}
            });
            for(int i=0;i<table.getColumnCount();i++)table.getColumnModel().getColumn(i).setPreferredWidth(65);
            fitColumns(table.getParent().getWidth());highlightCurrentInputs();
            info.setText(result.rows().length+I18n.t(" combinações • ")+name+I18n.t(" • tabela atualizada"));
            export.setEnabled(true);
          }catch(Exception e){
            Throwable cause=e.getCause()==null?e:e.getCause();
            info.setText(cause.getMessage());info.setToolTipText(cause.getMessage());
            if(showErrors)JOptionPane.showMessageDialog(owner,cause.getMessage(),I18n.t("Tabela verdade"),JOptionPane.INFORMATION_MESSAGE);
          }
        }
      };worker.execute();
    }catch(Exception e){
      info.setText(e.getMessage());info.setToolTipText(e.getMessage());
      if(showErrors)JOptionPane.showMessageDialog(owner,e.getMessage(),I18n.t("Tabela verdade"),JOptionPane.INFORMATION_MESSAGE);
    }
  }
  void refreshLanguage(){
    info.putClientProperty("i18n.text",null);
    if(result!=null)info.setText(result.rows().length+I18n.t(" combinações • ")+project.getCurrentCircuit().getName()+I18n.t(" • tabela atualizada"));
    else info.setText(worker==null?I18n.t("Calcule a tabela do circuito atual."):I18n.t("Calculando…"));
  }
  private void fitColumns(int viewportWidth){
    table.setAutoResizeMode(table.getColumnCount()*60<=viewportWidth?
        JTable.AUTO_RESIZE_ALL_COLUMNS:JTable.AUTO_RESIZE_OFF);
  }
  void highlightCurrentInputs(){
    if(result==null||project.getCurrentCircuit()==null)return;
    int row=0;
    for(var pin:Analyze.getPinLabels(project.getCurrentCircuit()).keySet()){
      if(!Pin.FACTORY.isInputPin(pin))continue;
      var value=Pin.FACTORY.getValue(project.getCircuitState().getInstanceState(pin));
      if(!value.isFullyDefined()){row=-1;break;}
      row=(row<<value.getWidth())|(int)value.toLongValue();
    }
    if(row>=result.rows().length)row=-1;
    if(currentInputRow!=row){currentInputRow=row;table.repaint();}
  }
  private void export(){
    if(result==null)return;
    JFileChooser chooser=new JFileChooser();
    chooser.setSelectedFile(new File("tabela-verdade.csv"));
    if(chooser.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION)return;
    Path path=chooser.getSelectedFile().toPath();
    if(Files.exists(path)&&JOptionPane.showConfirmDialog(owner,I18n.t("Substituir o arquivo existente?"),
        I18n.t("Exportar CSV"),JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
    try{
      StringBuilder csv=new StringBuilder("\ufeff");
      csv.append(String.join(";",result.headers())).append("\r\n");
      for(String[] row:result.rows())csv.append(String.join(";",row)).append("\r\n");
      Files.writeString(path,csv,StandardCharsets.UTF_8);info.setText(I18n.t("CSV salvo: ")+path.getFileName());
    }catch(IOException e){JOptionPane.showMessageDialog(owner,e.getMessage(),I18n.t("Exportar CSV"),JOptionPane.ERROR_MESSAGE);}
  }
  void dispose(){
    disposed=true;generation++;recalculate.stop();if(worker!=null)worker.cancel(false);project.removeProjectListener(listener);
  }
}
