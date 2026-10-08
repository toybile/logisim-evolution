package local.logisim.panels;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.comp.*;
import com.cburch.logisim.std.wiring.*;
import com.cburch.logisim.data.*;
import com.cburch.logisim.circuit.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
public final class PinAlignmentChecks {
  static int checks;
  static double maximum;
  static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  static double[] center(BufferedImage image, int rgb, double cx,double cy,double scale,boolean digit,boolean output,int background){
    int minX=image.getWidth(),maxX=-1,minY=image.getHeight(),maxY=-1;
    for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
      if(digit && Math.hypot((x+.5-cx)/scale,(y+.5-cy)/scale)>6.5)continue;
      int pixel=image.getRGB(x,y);
      if((pixel>>>24)<128)continue;
      if(rgb==0xffffff){int red=pixel>>16&255,bgRed=background>>16&255;if(red<bgRed+(255-bgRed)*.3)continue;}
      else if((pixel&0xffffff)!=rgb)continue;
      minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
    }
    check(maxX>=minX,"desenho ausente: "+Integer.toHexString(rgb));
    return new double[]{(minX+maxX+1)/2.0,(minY+maxY+1)/2.0};
  }
  public static void main(String[] args)throws Exception{
    Probe.setModernPinValues(true); Value.falseColor=new Color(122,132,149);Value.trueColor=new Color(0,155,78);
    var project=Checks.demo();var circuit=project.getCurrentCircuit();
    try{
      for(boolean input:new boolean[]{true,false})for(var dir:new Direction[]{Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH})for(int bit=0;bit<2;bit++){
        var attrs=Pin.FACTORY.createAttributeSet();attrs.setValue(Pin.ATTR_TYPE,input?Pin.INPUT:Pin.OUTPUT);
        attrs.setValue(StdAttr.FACING,dir);attrs.setValue(ProbeAttributes.PROBEAPPEARANCE,ProbeAttributes.PROBEAPPEARANCE.parse("classic"));
        var component=(InstanceComponent)Pin.FACTORY.createComponent(Location.create(100,100,true),attrs);
        var mutation=new CircuitMutation(circuit);mutation.add(component);mutation.execute();
        var state=project.getCircuitState().getInstanceState(component.getInstance());
        var value=bit==0?Value.FALSE:Value.TRUE;Pin.FACTORY.driveInputPin(state,value);
        for(double scale:new double[]{1,1.25,1.5,2}){
          var image=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
          g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
          g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
          var bounds=component.getBounds();g.scale(scale,scale);g.translate(20-bounds.getX(),20-bounds.getY());
          var context=new ComponentDrawContext(null,circuit,project.getCircuitState(),g,g);context.setShowState(true);
          component.draw(context);g.dispose();
          double cx=(20+bounds.getWidth()/2.0+1)*scale,cy=(20+bounds.getHeight()/2.0+1)*scale;
          String label=(input?"entrada":"saída")+" "+dir+" "+bit+" zoom "+scale;
          var digit=center(image,input?0xffffff:0,cx,cy,scale,true,!input,value.getColor().getRGB());
          if(dir==Direction.EAST&&input&&scale==1)ImageIO.write(image,"png",Paths.get(args[0],"entrada-"+bit+".png").toFile());
          double digitError=Math.max(Math.abs(digit[0]-cx),Math.abs(digit[1]-cy));maximum=Math.max(maximum,digitError);
          check(digitError<=.65,"número fora do centro: "+label+" erro "+digitError);
          if(input){
            var circle=center(image,value.getColor().getRGB()&0xffffff,cx,cy,scale,false,false,value.getColor().getRGB());
            double circleError=Math.max(Math.abs(circle[0]-cx),Math.abs(circle[1]-cy));maximum=Math.max(maximum,circleError);
            check(circleError<=.65,"círculo fora do centro: "+label+" erro "+circleError);
          }
          if(dir==Direction.EAST&&input&&bit==0&&scale==2)ImageIO.write(image,"png",Paths.get(args[0],"entrada.png").toFile());
          if(dir==Direction.WEST&&!input&&bit==1&&scale==2)ImageIO.write(image,"png",Paths.get(args[0],"saida.png").toFile());
        }
      }
      System.out.println("PASSOU: "+checks+" verificações; maior diferença de rasterização: "+maximum+" pixel");
    }finally{project.getSimulator().shutDown();project.getLogisimFile().stopAutosaveThread(false);}
    System.exit(0);
  }
}
