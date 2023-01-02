package code.snippets.client;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import code.snippets.models.SnippetCategory;

public class CategoryRenderer implements ListCellRenderer<SnippetCategory> {
  
  public static final int ICON_SPACER = 5;
  
  private IconLoader loader;
  
  public CategoryRenderer(IconLoader loader) {
    this.loader = loader;
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends SnippetCategory> list, SnippetCategory value, int index,
      boolean isSelected, boolean cellHasFocus) {
    JPanel ret = new JPanel();
    
    if( index == -1 ) {
      ret.setOpaque(false);
    } else if( isSelected ) {
      ret.setOpaque(true);
      ret.setBackground(list.getSelectionBackground());
      ret.setForeground(list.getSelectionForeground());
    } else {
      ret.setOpaque(true);
      ret.setBackground(list.getBackground());
      ret.setForeground(list.getForeground());
    }
    
    if( value.icon != null && index != -1 ) {
      JLabel icon = new JLabel();
      ret.add(icon);
      ret.add(Box.createRigidArea(new Dimension(CategoryRenderer.ICON_SPACER, 0)));
      
      BufferedImage img = loader.getIcon(value.icon);
      if( img != null ) {
        icon.setIcon(new ImageIcon(img));
      }
    } else if( value.icon == null && index != -1 ) {
      ret.add(Box.createRigidArea(new Dimension(0, 32))); // TODO: do 16 x 16
    }
    
    ret.setLayout(new BoxLayout(ret, BoxLayout.LINE_AXIS));
    JLabel name = new JLabel(value.name);
    ret.add(name);
    
    ret.add(Box.createHorizontalGlue());
    
    ret.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    
    return ret;
  }

}
