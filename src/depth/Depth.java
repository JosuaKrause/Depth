package depth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import depth.Picture3D.RenderMode;

public class Depth extends JFrame {

  private static final long serialVersionUID = 2045195245929930615L;

  protected int mode = 1;

  protected Picture3D pic;

  protected int xPos;

  protected int yPos;

  protected int radius = 30;

  protected Picture depth;

  protected Picture img;

  private final JComponent comp;

  private class PicAction extends AbstractAction {

    private static final long serialVersionUID = -5195912606444403591L;

    private final int setMode;

    public PicAction(final int setMode) {
      this.setMode = setMode;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if(setMode == 1) {
        pic.update();
      }
      mode = setMode;
      comp.repaint();
    }

  }

  public Depth(final BufferedImage i, final BufferedImage d) {
    pic = new Picture3D(i, d);
    pic.update();
    depth = new Picture(d);
    img = new Picture(i);
    comp = new JComponent() {

      private static final long serialVersionUID = 2863632496156500738L;

      {
        setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
        setFocusable(true);
        addAction(KeyEvent.VK_1, new PicAction(1));
        addAction(KeyEvent.VK_2, new PicAction(2));
        addAction(KeyEvent.VK_3, new PicAction(3));
        addAction(KeyEvent.VK_R, new AbstractAction() {

          private static final long serialVersionUID = -8571874798155653538L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            final RenderMode m = pic.getRenderMode();
            final RenderMode[] values = RenderMode.values();
            RenderMode next = values[0];
            int i = values.length;
            while(--i >= 0) {
              if(values[i] == m) {
                break;
              }
              next = values[i];
            }
            pic.setRenderMode(next);
            repaint();
          }

        });
        addAction(KeyEvent.VK_Q, new AbstractAction() {

          private static final long serialVersionUID = 3405492483455727601L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            Depth.this.dispose();
          }

        });
      }

      private void addAction(final int key, final Action a) {
        final Object token = new Object();
        final InputMap input = getInputMap();
        input.put(KeyStroke.getKeyStroke(key, 0), token);
        final ActionMap action = getActionMap();
        action.put(token, a);
      }

      @Override
      public void paint(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        Depth.this.paint(g2);
        g2.dispose();
        final Graphics2D g3 = (Graphics2D) g.create();
        g3.setColor(Color.BLACK);
        g3.draw(new Ellipse2D.Double(xPos - radius, yPos - radius, radius * 2,
            radius * 2));
        g3.dispose();
      }

    };
    final MouseAdapter mouse = new MouseAdapter() {

      @Override
      public void mouseMoved(final MouseEvent e) {
        xPos = e.getX();
        yPos = e.getY();
        comp.repaint();
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        edit(e);
        comp.grabFocus();
      }

      @Override
      public void mouseDragged(final MouseEvent e) {
        edit(e);
      }

      private void edit(final MouseEvent e) {
        xPos = e.getX();
        yPos = e.getY();
        depth.editDepth(xPos, yPos, radius,
            e.getButton() == MouseEvent.BUTTON1 ? 25.0 : -25.0, pic);
        comp.repaint();
      }

      @Override
      public void mouseWheelMoved(final MouseWheelEvent e) {
        final int rot = e.getWheelRotation();
        radius += rot * 5;
        if(radius > 100) {
          radius = 100;
        }
        if(radius < 1) {
          radius = 1;
        }
        comp.repaint();
      }

    };
    comp.addMouseListener(mouse);
    comp.addMouseMotionListener(mouse);
    comp.addMouseWheelListener(mouse);
    setLayout(new BorderLayout());
    add(comp);
    pack();
    setLocationRelativeTo(null);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  public static void main(final String[] args) throws IOException {
    final String ext = ".jpg";
    final String name = "mountain";
    final BufferedImage img = ImageIO.read(new File(name + ext));
    final BufferedImage depth = ImageIO.read(new File(name + "_depth" + ext));
    final Depth d = new Depth(img, depth);
    d.setVisible(true);
  }

  protected void paint(final Graphics2D g) {
    switch(mode) {
      case 1:
        pic.draw(g);
        break;
      case 2:
        img.draw(g);
        break;
      case 3:
        depth.draw(g);
        break;
    }
  }

}
