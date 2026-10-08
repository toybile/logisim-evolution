package local.logisim.panels;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.generic.GridPainter;
import java.awt.Color;

/** Process-local presentation colors; native preference values are preserved. */
final class CanvasSkin {
  final Canvas canvas;
  final GridPainter grid;
  final Color oldFalse=Value.falseColor, oldTrue=Value.trueColor;
  final Color oldBackground;
  CanvasSkin(Canvas canvas) {
    this.canvas=canvas;
    grid=canvas.getGridPainter();
    oldBackground=canvas.getBackground();
    com.cburch.logisim.std.wiring.Probe.setModernPinValues(true);
    Value.falseColor=new Color(122,132,149);
    Value.trueColor=Appearance.signalOne();
    refresh();
  }
  void refresh() {
    Color background=Appearance.canvas();
    if(!canvas.getBackground().equals(background))canvas.setBackground(background);
    grid.setPalette(background,Appearance.mix(background,Appearance.accent(),.23f));
  }
  void restore() {
    canvas.setBackground(oldBackground);
    com.cburch.logisim.std.wiring.Probe.setModernPinValues(false);
    Value.falseColor=oldFalse;
    Value.trueColor=oldTrue;
    grid.setPalette(null,null);
  }
}
