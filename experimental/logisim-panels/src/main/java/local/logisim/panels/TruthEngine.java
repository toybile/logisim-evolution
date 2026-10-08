package local.logisim.panels;

import com.cburch.logisim.analyze.model.*;
import com.cburch.logisim.circuit.*;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.wiring.Pin;
import java.util.*;

final class TruthEngine {
  record Result(String[] headers,String[][] rows,AnalyzerModel model,String equation){}
  static Result calculate(Project project,Circuit circuit) {
    if(circuit==null)throw new IllegalArgumentException(I18n.t("Selecione um circuito."));
    checkCombinational(circuit,new HashSet<>());
    Map<Instance,String> labels=Analyze.getPinLabels(circuit);
    List<Var> inputs=new ArrayList<>(),outputs=new ArrayList<>();
    int inputBits=0,outputBits=0;
    for(var entry:labels.entrySet()){
      Instance pin=entry.getKey();int width=Pin.FACTORY.getWidth(pin).getWidth();
      if(Pin.FACTORY.isInputPin(pin)){inputs.add(new Var(entry.getValue(),width));inputBits+=width;}
      else{outputs.add(new Var(entry.getValue(),width));outputBits+=width;}
    }
    if(outputs.isEmpty())throw new IllegalArgumentException(I18n.t("Adicione ao menos um pino de saída ao circuito."));
    if(inputBits>10)throw new IllegalArgumentException(
        I18n.t("Este painel aceita até 10 bits de entrada (1.024 combinações). Use Projeto → Analisar circuito para ampliar a análise."));
    if(outputBits>AnalyzerModel.MAX_OUTPUTS)throw new IllegalArgumentException(I18n.t("Há mais bits de saída do que o analisador suporta."));
    AnalyzerModel model=new AnalyzerModel();model.setCurrentCircuit(project,circuit);
    model.setVariables(inputs,outputs);
    Analyze.computeTable(model,project,circuit,labels);
    TruthTable table=model.getTruthTable();int count=table.getRowCount();
    String[] headers=new String[inputBits+outputBits];String[][] rows=new String[count][headers.length];
    for(int i=0;i<inputBits;i++)headers[i]=table.getInputHeader(i);
    for(int i=0;i<outputBits;i++)headers[inputBits+i]=table.getOutputHeader(i);
    for(int row=0;row<count;row++){
      for(int col=0;col<inputBits;col++)rows[row][col]=table.getInputEntry(row,col).toBitString();
      for(int col=0;col<outputBits;col++)rows[row][inputBits+col]=table.getOutputEntry(row,col).toBitString();
    }
    try{Analyze.computeExpression(model,circuit,labels);}catch(AnalyzeException ignored){}
    StringJoiner equations=new StringJoiner(";  ");
    for(String output:model.getOutputs().bits){
      var expression=model.getOutputExpressions().getExpression(output);
      if(expression!=null)equations.add(output+" = "+expression.toString(Expression.Notation.LOGIC));
    }
    return new Result(headers,rows,model,equations.toString());
  }
  static void checkCombinational(Circuit circuit,Set<Circuit> visited){
    if(!visited.add(circuit))return;
    for(var component:circuit.getNonWires()){
      var factory=component.getFactory();String type=factory.getClass().getName();
      if(factory instanceof SubcircuitFactory sub){checkCombinational(sub.getSubcircuit(),visited);continue;}
      if(type.contains(".std.memory.")||type.contains(".soc.")||type.contains(".vhdl.")||
          type.endsWith(".Clock")||type.contains(".std.tcl.")){
        throw new IllegalArgumentException(
            I18n.t("Este circuito depende de memória, relógio ou código externo. Use a Simulação para acompanhar seus estados."));
      }
    }
  }
}
