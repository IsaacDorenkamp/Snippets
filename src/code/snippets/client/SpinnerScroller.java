package code.snippets.client;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class SpinnerScroller implements MouseWheelListener {
  
  private JSpinner spinner;
  private SpinnerNumberModel model;
  private Callback<Integer> onChange;
  
  public SpinnerScroller(JSpinner spinner, Callback<Integer> onChange) {
    assert spinner.getModel() instanceof SpinnerNumberModel;
    this.spinner = spinner;
    this.model = (SpinnerNumberModel) spinner.getModel();
    this.onChange = onChange;
  }
  
  public SpinnerScroller(JSpinner spinner) {
    this(spinner, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void mouseWheelMoved(MouseWheelEvent evt) {
    int rot = -(Integer)model.getStepSize()*evt.getWheelRotation();
    
    Integer newSize = (Integer)spinner.getValue() + rot;
    
    if( ((Comparable<Integer>)model.getMinimum()).compareTo(newSize) <= 0 &&
        ((Comparable<Integer>)model.getMaximum()).compareTo((Integer)newSize) >= 0 ) {
      spinner.setValue(newSize);
      if( onChange != null ) onChange.invoke(newSize);
    }
  }

}
