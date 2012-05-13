package depth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFrame;

import depth.Picture3D.RenderMode;

public class Depth extends JFrame implements Painter {

  private static final long serialVersionUID = 2045195245929930615L;

  protected int mode = 1;

  protected Picture3D pic;

  protected int xPos;

  protected int yPos;

  protected int radius = 30;

  protected Picture depth;

  protected Picture img;

  protected boolean drawMode = false;

  private final Canvas comp;

  private class PicAction extends AbstractAction {

    private static final long serialVersionUID = -5195912606444403591L;

    private final int setMode;

    public PicAction(final int setMode) {
      this.setMode = setMode;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if(setMode == 1) {
        pic.update(comp);
      }
      mode = setMode;
      comp.repaint();
    }

  }

  public Depth(final BufferedImage i, final BufferedImage d) {
    pic = new Picture3D(i, d);
    depth = new Picture(d);
    img = new Picture(i);
    comp = new Canvas(this, img.getWidth(), img.getHeight()) {

      private static final long serialVersionUID = 2863632496156500738L;

      @Override
      public void setupKeyActions() {
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
            pic.setRenderMode(next, comp);
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
        addAction(KeyEvent.VK_M, new AbstractAction() {

          private static final long serialVersionUID = 4130158851209389262L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            drawMode = isMoveable();
            setMoveable(!drawMode);
            repaint();
          }

        });
        addAction(KeyEvent.VK_V, new AbstractAction() {

          private static final long serialVersionUID = -7512759810216951365L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            reset(new Rectangle2D.Double(0, 0, pic.getWidth(), pic.getHeight()));
          }

        });
        setMargin(0);
      }

    };
    final MouseAdapter mouse = new MouseAdapter() {

      @Override
      public void mouseMoved(final MouseEvent e) {
        if(drawMode) {
          xPos = e.getX();
          yPos = e.getY();
          comp.repaint();
        }
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        if(drawMode) {
          edit(e);
          comp.grabFocus();
        }
      }

      @Override
      public void mouseDragged(final MouseEvent e) {
        if(drawMode) {
          edit(e);
        }
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
        if(drawMode) {
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
    d.pic.update(d.comp);
  }

  @Override
  public void draw(final Graphics2D g) {
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

  @Override
  public void drawStatic(final Graphics2D g) {
    if(!drawMode) {
      return;
    }
    g.setColor(Color.BLACK);
    g.draw(new Ellipse2D.Double(xPos - radius, yPos - radius, radius * 2,
        radius * 2));
  }

}
