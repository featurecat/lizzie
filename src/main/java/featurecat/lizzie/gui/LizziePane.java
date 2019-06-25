package featurecat.lizzie.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/** The window used to display the game. */
public class LizziePane extends JPanel {

  private static final String uiClassID = "LizziePaneUI";

  static {
    UIManager.put(uiClassID, BasicLizziePaneUI.class.getName());
  }

  private boolean floatable = true;

  /** Keys to lookup borders in defaults table. */
  private static final int[] cursorMapping =
      new int[] {
        Cursor.NW_RESIZE_CURSOR,
        Cursor.NW_RESIZE_CURSOR,
        Cursor.N_RESIZE_CURSOR,
        Cursor.NE_RESIZE_CURSOR,
        Cursor.NE_RESIZE_CURSOR,
        Cursor.NW_RESIZE_CURSOR,
        0,
        0,
        0,
        Cursor.NE_RESIZE_CURSOR,
        Cursor.W_RESIZE_CURSOR,
        0,
        0,
        0,
        Cursor.E_RESIZE_CURSOR,
        Cursor.SW_RESIZE_CURSOR,
        0,
        0,
        0,
        Cursor.SE_RESIZE_CURSOR,
        Cursor.SW_RESIZE_CURSOR,
        Cursor.SW_RESIZE_CURSOR,
        Cursor.S_RESIZE_CURSOR,
        Cursor.SE_RESIZE_CURSOR,
        Cursor.SE_RESIZE_CURSOR
      };

  /** The amount of space (in pixels) that the cursor is changed on. */
  private static final int CORNER_DRAG_WIDTH = 16;

  /** Region from edges that dragging is active from. */
  private static final int BORDER_DRAG_THICKNESS = 5;

  /**
   * <code>Cursor</code> used to track the cursor set by the user. This is initially <code>
   * Cursor.DEFAULT_CURSOR</code>.
   */
  private Cursor lastCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

  protected PaneDragListener dragListener;
  protected Input input;

  public LizziePane() {
    super();
  }

  /** Creates a window */
  public LizziePane(LizzieMain owner) {
    //    super(owner);
    //    initCompotents();
    //    input = owner.input;
    //    installInputListeners();
    setOpaque(false);
  }

  @Override
  public LizziePaneUI getUI() {
    return (LizziePaneUI) ui;
  }

  public void setUI(LizziePaneUI ui) {
    super.setUI(ui);
  }

  public void updateUI() {
    setUI((LizziePaneUI) UIManager.getUI(this));
    if (getLayout() == null) {
      setLayout(new DefaultLizziePaneLayout());
    }
    invalidate();
  }

  public String getUIClassID() {
    return uiClassID;
  }

  public void toWindow(Point position, Dimension size) {
    if (getUI() != null) {
      getUI().toWindow(position, size);
    }
  }

  public int getComponentIndex(Component c) {
    int ncomponents = this.getComponentCount();
    Component[] component = this.getComponents();
    for (int i = 0; i < ncomponents; i++) {
      Component comp = component[i];
      if (comp == c) return i;
    }
    return -1;
  }

  public Component getComponentAtIndex(int i) {
    int ncomponents = this.getComponentCount();
    if (i >= 0 && i < ncomponents) {
      Component[] component = this.getComponents();
      return component[i];
    }
    return null;
  }

  public boolean isFloatable() {
    return floatable;
  }

  public void setFloatable(boolean b) {
    if (floatable != b) {
      boolean old = floatable;
      floatable = b;

      firePropertyChange("floatable", old, b);
      revalidate();
      repaint();
    }
  }

  private void initCompotents() {
    setBorder(BorderFactory.createEmptyBorder());
    setVisible(true);
  }

  private class PaneDragListener extends MouseAdapter {

    /** Set to true if the drag operation is moving the window. */
    private boolean isMovingWindow;

    /** Used to determine the corner the resize is occurring from. */
    private int dragCursor;

    /** X location the mouse went down on for a drag operation. */
    private int dragOffsetX;

    /** Y location the mouse went down on for a drag operation. */
    private int dragOffsetY;

    /** Width of the window when the drag started. */
    private int dragWidth;

    /** Height of the window when the drag started. */
    private int dragHeight;

    /** Window the <code>JRootPane</code> is in. */
    private Window window;

    public PaneDragListener(Window window) {
      this.window = window;
    }

    public void mouseMoved(MouseEvent e) {
      Window w = (Window) e.getSource();

      JWindow f = null;
      JDialog d = null;

      if (w instanceof JWindow) {
        f = (JWindow) w;
      } else if (w instanceof JDialog) {
        d = (JDialog) w;
      }

      // Update the cursor
      int cursor = getCursor(calculateCorner(w, e.getX(), e.getY()));

      if (cursor != 0
          && ((f != null) // && (f.isResizable() && (f.getExtendedState() & Frame.MAXIMIZED_BOTH)
              // == 0))
              || (d != null && d.isResizable()))) {
        w.setCursor(Cursor.getPredefinedCursor(cursor));
      } else {
        w.setCursor(lastCursor);
      }
    }

    public void mouseReleased(MouseEvent e) {
      if (dragCursor != 0 && window != null && !window.isValid()) {
        // Some Window systems validate as you resize, others won't,
        // thus the check for validity before repainting.
        window.validate();
        getRootPane().repaint();
      }
      isMovingWindow = false;
      dragCursor = 0;
    }

    public void mousePressed(MouseEvent e) {
      Point dragWindowOffset = e.getPoint();
      Window w = (Window) e.getSource();
      if (w != null) {
        w.toFront();
      }

      JWindow f = null;
      JDialog d = null;

      if (w instanceof JWindow) {
        f = (JWindow) w;
      } else if (w instanceof JDialog) {
        d = (JDialog) w;
      }

      // int frameState = (f != null) ? f.getExtendedState() : 0;

      if (((f != null) // && ((frameState & Frame.MAXIMIZED_BOTH) == 0)
              || (d != null))
          && dragWindowOffset.y >= BORDER_DRAG_THICKNESS
          && dragWindowOffset.x >= BORDER_DRAG_THICKNESS
          && dragWindowOffset.x < w.getWidth() - BORDER_DRAG_THICKNESS) {
        isMovingWindow = true;
        dragOffsetX = dragWindowOffset.x;
        dragOffsetY = dragWindowOffset.y;
      } else if (f != null // && f.isResizable() && ((frameState & Frame.MAXIMIZED_BOTH) == 0)
          || (d != null && d.isResizable())) {
        dragOffsetX = dragWindowOffset.x;
        dragOffsetY = dragWindowOffset.y;
        dragWidth = w.getWidth();
        dragHeight = w.getHeight();
        dragCursor = getCursor(calculateCorner(w, dragWindowOffset.x, dragWindowOffset.y));
      }
    }

    public void mouseDragged(MouseEvent e) {
      Window w = (Window) e.getSource();
      Point pt = e.getPoint();

      if (isMovingWindow) {
        Point eventLocationOnScreen = e.getLocationOnScreen();
        w.setLocation(eventLocationOnScreen.x - dragOffsetX, eventLocationOnScreen.y - dragOffsetY);
      } else if (dragCursor != 0) {
        Rectangle r = w.getBounds();
        Rectangle startBounds = new Rectangle(r);
        Dimension min = w.getMinimumSize();

        switch (dragCursor) {
          case Cursor.E_RESIZE_CURSOR:
            adjust(r, min, 0, 0, pt.x + (dragWidth - dragOffsetX) - r.width, 0);
            break;
          case Cursor.S_RESIZE_CURSOR:
            adjust(r, min, 0, 0, 0, pt.y + (dragHeight - dragOffsetY) - r.height);
            break;
          case Cursor.N_RESIZE_CURSOR:
            adjust(r, min, 0, pt.y - dragOffsetY, 0, -(pt.y - dragOffsetY));
            break;
          case Cursor.W_RESIZE_CURSOR:
            adjust(r, min, pt.x - dragOffsetX, 0, -(pt.x - dragOffsetX), 0);
            break;
          case Cursor.NE_RESIZE_CURSOR:
            adjust(
                r,
                min,
                0,
                pt.y - dragOffsetY,
                pt.x + (dragWidth - dragOffsetX) - r.width,
                -(pt.y - dragOffsetY));
            break;
          case Cursor.SE_RESIZE_CURSOR:
            adjust(
                r,
                min,
                0,
                0,
                pt.x + (dragWidth - dragOffsetX) - r.width,
                pt.y + (dragHeight - dragOffsetY) - r.height);
            break;
          case Cursor.NW_RESIZE_CURSOR:
            adjust(
                r,
                min,
                pt.x - dragOffsetX,
                pt.y - dragOffsetY,
                -(pt.x - dragOffsetX),
                -(pt.y - dragOffsetY));
            break;
          case Cursor.SW_RESIZE_CURSOR:
            adjust(
                r,
                min,
                pt.x - dragOffsetX,
                0,
                -(pt.x - dragOffsetX),
                pt.y + (dragHeight - dragOffsetY) - r.height);
            break;
          default:
            break;
        }
        if (!r.equals(startBounds)) {
          w.setBounds(r);
          // Defer repaint/validate on mouseReleased unless dynamic
          // layout is active.
          if (Toolkit.getDefaultToolkit().isDynamicLayoutActive()) {
            w.validate();
            getRootPane().repaint();
          }
        }
      }
    }

    private int calculateCorner(Window c, int x, int y) {
      Insets insets = c.getInsets();
      int xPosition = calculatePosition(x - insets.left, c.getWidth() - insets.left - insets.right);
      int yPosition = calculatePosition(y - insets.top, c.getHeight() - insets.top - insets.bottom);

      if (xPosition == -1 || yPosition == -1) {
        return -1;
      }
      return yPosition * 5 + xPosition;
    }

    private int getCursor(int corner) {
      if (corner == -1) {
        return 0;
      }
      return cursorMapping[corner];
    }

    private int calculatePosition(int spot, int width) {
      if (spot < BORDER_DRAG_THICKNESS) {
        return 0;
      }
      if (spot < CORNER_DRAG_WIDTH) {
        return 1;
      }
      if (spot >= (width - BORDER_DRAG_THICKNESS)) {
        return 4;
      }
      if (spot >= (width - CORNER_DRAG_WIDTH)) {
        return 3;
      }
      return 2;
    }

    private void adjust(
        Rectangle bounds, Dimension min, int deltaX, int deltaY, int deltaWidth, int deltaHeight) {
      bounds.x += deltaX;
      bounds.y += deltaY;
      bounds.width += deltaWidth;
      bounds.height += deltaHeight;
      if (min != null) {
        if (bounds.width < min.width) {
          int correction = min.width - bounds.width;
          if (deltaX != 0) {
            bounds.x -= correction;
          }
          bounds.width = min.width;
        }
        if (bounds.height < min.height) {
          int correction = min.height - bounds.height;
          if (deltaY != 0) {
            bounds.y -= correction;
          }
          bounds.height = min.height;
        }
      }
    }
  }

  protected void installDesignListeners() {
    LizziePaneUI ui = getUI();
    if (ui != null && ui instanceof BasicLizziePaneUI) {
      ((BasicLizziePaneUI) ui).installListeners();
    }
  }

  protected void uninstallDesignListeners() {
    LizziePaneUI ui = getUI();
    if (ui != null && ui instanceof BasicLizziePaneUI) {
      ((BasicLizziePaneUI) ui).uninstallListeners();
    }
  }

  protected void installInputListeners() {
    //    addMouseListener(input);
    //    addKeyListener(input);
    //    addMouseWheelListener(input);
    //    addMouseMotionListener(input);
  }

  protected void uninstallInputListeners() {
    //    removeMouseListener(input);
    //    removeKeyListener(input);
    //    removeMouseWheelListener(input);
    //    removeMouseMotionListener(input);
  }

  public void setDesignMode(boolean mode) {
    if (mode) {
      uninstallInputListeners();
      installDesignListeners();
    } else {
      uninstallDesignListeners();
      installInputListeners();
    }
  }

  private class DefaultLizziePaneLayout
      implements LayoutManager2, Serializable, PropertyChangeListener, UIResource {

    LizzieLayout lm;

    DefaultLizziePaneLayout() {
      lm = new LizzieLayout();
    }

    /** @deprecated replaced by <code>addLayoutComponent(Component, Object)</code>. */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
      lm.addLayoutComponent(name, comp);
    }

    public void addLayoutComponent(Component comp, Object constraints) {
      lm.addLayoutComponent(comp, constraints);
    }

    public void removeLayoutComponent(Component comp) {
      lm.removeLayoutComponent(comp);
    }

    public Dimension preferredLayoutSize(Container target) {
      return lm.preferredLayoutSize(target);
    }

    public Dimension minimumLayoutSize(Container target) {
      return lm.minimumLayoutSize(target);
    }

    public Dimension maximumLayoutSize(Container target) {
      return lm.maximumLayoutSize(target);
    }

    public void layoutContainer(Container target) {
      lm.layoutContainer(target);
    }

    public float getLayoutAlignmentX(Container target) {
      return lm.getLayoutAlignmentX(target);
    }

    public float getLayoutAlignmentY(Container target) {
      return lm.getLayoutAlignmentY(target);
    }

    public void invalidateLayout(Container target) {
      lm.invalidateLayout(target);
    }

    public void propertyChange(PropertyChangeEvent e) {
      // TODO
      //      String name = e.getPropertyName();
      //      if (name.equals("orientation")) {
      //        int o = ((Integer) e.getNewValue()).intValue();
      //                if (o == LizziePane.VERTICAL)
      //                    lm = new LizzieLayout(LizziePane.this, LizzieLayout.PAGE_AXIS);
      //                else {
      //                    lm = new LizzieLayout(LizziePane.this, LizzieLayout.LINE_AXIS);
      //                }
      //      }
    }
  }

  public void setLayout(LayoutManager mgr) {
    LayoutManager oldMgr = getLayout();
    if (oldMgr instanceof PropertyChangeListener) {
      removePropertyChangeListener((PropertyChangeListener) oldMgr);
    }
    super.setLayout(mgr);
  }

  private void writeObject(ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    if (getUIClassID().equals(uiClassID)) {
      //        byte count = JComponent.getWriteObjCounter(this);
      //        JComponent.setWriteObjCounter(this, --count);
      //        if (count == 0 && ui != null) {
      ui.installUI(this);
      //        }
    }
  }

  public static class HtmlKit extends HTMLEditorKit {
    private StyleSheet style = new StyleSheet();

    @Override
    public void setStyleSheet(StyleSheet styleSheet) {
      style = styleSheet;
    }

    @Override
    public StyleSheet getStyleSheet() {
      if (style == null) {
        style = super.getStyleSheet();
      }
      return style;
    }
  }
}
