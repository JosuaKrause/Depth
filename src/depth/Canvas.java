package depth;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class Canvas extends JComponent {

  private static final long serialVersionUID = 5148536262867772166L;

  private final Painter painter;

  private double offX;

  private double offY;

  private double zoom = 1;

  public Canvas(final Painter painter, final int width, final int height) {
    this.painter = painter;
    setPreferredSize(new Dimension(width, height));
    final MouseAdapter mouse = new MouseAdapter() {

      private boolean drag;

      private int startx;

      private int starty;

      private double origX;

      private double origY;

      @Override
      public void mousePressed(final MouseEvent e) {
        if(isMoveable() && e.getButton() == MouseEvent.BUTTON1) {
          startx = e.getX();
          starty = e.getY();
          origX = getOffsetX();
          origY = getOffsetY();
          drag = true;
        }
        grabFocus();
      }

      @Override
      public void mouseDragged(final MouseEvent e) {
        if(drag) {
          move(e.getX(), e.getY());
        }
      }

      @Override
      public void mouseReleased(final MouseEvent e) {
        if(drag) {
          move(e.getX(), e.getY());
          drag = false;
        }
      }

      /**
       * sets the offset according to the mouse position
       * 
       * @param x the mouse x position
       * @param y the mouse y position
       */
      private void move(final int x, final int y) {
        setOffset(origX + (x - startx), origY + (y - starty));
      }

      @Override
      public void mouseWheelMoved(final MouseWheelEvent e) {
        if(isMoveable()) {
          zoomTo(e.getX(), e.getY(), e.getWheelRotation());
        }
      }

    };
    addMouseListener(mouse);
    addMouseMotionListener(mouse);
    addMouseWheelListener(mouse);
    setFocusable(true);
    grabFocus();
    setupKeyActions();
  }

  public void setupKeyActions() {
    // to be overwritten
  }

  protected void addAction(final int key, final Action a) {
    final Object token = new Object();
    final InputMap input = getInputMap();
    input.put(KeyStroke.getKeyStroke(key, 0), token);
    final ActionMap action = getActionMap();
    action.put(token, a);
  }

  private Color back;

  @Override
  public void setBackground(final Color bg) {
    back = bg;
    super.setBackground(bg);
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g.create();
    g2.clip(getVisibleRect());
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    final Color c = back;
    if(c != null) {
      g2.setColor(c);
      g2.fill(getVisibleRect());
    }
    final Graphics2D gfx = (Graphics2D) g2.create();
    gfx.translate(offX, offY);
    gfx.scale(zoom, zoom);
    painter.draw(gfx);
    gfx.dispose();
    painter.drawStatic(g2);
    g2.dispose();
  }

  /**
   * Setter.
   * 
   * @param x the x offset.
   * @param y the y offset.
   */
  public void setOffset(final double x, final double y) {
    offX = x;
    offY = y;
    repaint();
  }

  /**
   * Zooms to the on screen (in components coordinates) position.
   * 
   * @param x The x coordinate.
   * @param y The y coordinate.
   * @param zooming The amount of zooming.
   */
  public void zoomTo(final double x, final double y, final int zooming) {
    final double factor = Math.pow(1.1, -zooming);
    zoomTo(x, y, factor);
  }

  /**
   * Zooms to the on screen (in components coordinates) position.
   * 
   * @param x The x coordinate.
   * @param y The y coordinate.
   * @param factor The factor to alter the zoom level.
   */
  public void zoomTo(final double x, final double y, final double factor) {
    // P = (off - mouse) / zoom
    // P = (newOff - mouse) / newZoom
    // newOff = (off - mouse) / zoom * newZoom + mouse
    // newOff = (off - mouse) * factor + mouse
    zoom *= factor;
    // does repaint
    setOffset((offX - x) * factor + x, (offY - y) * factor + y);
  }

  /**
   * Zooms towards the center of the display area.
   * 
   * @param factor The zoom factor.
   */
  public void zoom(final double factor) {
    final Rectangle box = getVisibleRect();
    zoomTo(box.width / 2.0, box.height / 2.0, factor);
  }

  /**
   * Getter.
   * 
   * @return the x offset
   */
  public double getOffsetX() {
    return offX;
  }

  /**
   * Getter.
   * 
   * @return the y offset
   */
  public double getOffsetY() {
    return offY;
  }

  public void reset() {
    reset(null);
  }

  public double margin = 10.0;

  public double getMargin() {
    return margin;
  }

  public void setMargin(final double margin) {
    this.margin = margin;
  }

  public void reset(final Rectangle2D bbox) {
    final Rectangle2D rect = getVisibleRect();
    if(bbox == null) {
      zoom = 1;
      setOffset(rect.getCenterX(), rect.getCenterY());
    } else {
      final int nw = (int) (rect.getWidth() - 2 * margin);
      final int nh = (int) (rect.getHeight() - 2 * margin);
      zoom = 1.0;
      // does repaint
      setOffset(margin + (nw - bbox.getWidth()) / 2 - bbox.getMinX(), margin
          + (nh - bbox.getHeight()) / 2 - bbox.getMinY());
      final double rw = nw / bbox.getWidth();
      final double rh = nh / bbox.getHeight();
      final double factor = rw < rh ? rw : rh;
      zoom(factor);
    }
  }

  private boolean isMoveable = true;

  public void setMoveable(final boolean isMoveable) {
    this.isMoveable = isMoveable;
  }

  public boolean isMoveable() {
    return isMoveable;
  }

}
