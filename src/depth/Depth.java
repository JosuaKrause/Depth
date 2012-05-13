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
import java.io.FileFilter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFrame;

import depth.Picture3D.RenderMode;

public class Depth extends JFrame implements Painter {

  public static final void main(final String[] args) throws IOException {
    final String folder = "examples";
    final Depth d = new Depth();
    d.setPicture(listImages(folder)[0]);
  }

  public static final String DEPTH = "_depth";

  public static final File getDepth(final File file) {
    final String name = file.getName();
    final int i = name.lastIndexOf('.');
    final String n = name.substring(0, i);
    final String e = name.substring(i);
    return new File(file.getParent(), n + DEPTH + e);
  }

  public static final File[] listImages(final String folder) {
    final File path = new File(folder);
    final FileFilter filter = new FileFilter() {

      @Override
      public boolean accept(final File f) {
        if(!f.isFile()) {
          return false;
        }
        final String name = f.getName();
        if(!name.endsWith(".png") && !name.endsWith(".jpg")
            && !name.endsWith(".jpeg")) {
          return false;
        }
        return getDepth(f).exists();
      }

    };
    return path.listFiles(filter);
  }

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

  private File file;

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

  private RenderMode renderMode;

  public Depth() {
    comp = new Canvas(this, 800, 600) {

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
            final RenderMode m = getRenderMode();
            final RenderMode[] values = RenderMode.values();
            RenderMode next = values[0];
            int i = values.length;
            while(--i >= 0) {
              if(values[i] == m) {
                break;
              }
              next = values[i];
            }
            setRenderMode(next);
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
        addAction(KeyEvent.VK_LEFT, new AbstractAction() {

          private static final long serialVersionUID = -6718270417186190777L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            try {
              prevPicture();
            } catch(final IOException io) {
              repaint();
            }
          }

        });
        addAction(KeyEvent.VK_RIGHT, new AbstractAction() {

          private static final long serialVersionUID = -8937019109230258124L;

          @Override
          public void actionPerformed(final ActionEvent e) {
            try {
              nextPicture();
            } catch(final IOException io) {
              repaint();
            }
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
    comp.setBackground(Color.BLACK);
    setLayout(new BorderLayout());
    add(comp);
    pack();
    setRenderMode(RenderMode.SORTED_BLUR_LOG);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  public void setRenderMode(final RenderMode renderMode) {
    this.renderMode = renderMode;
    setTitle(renderMode.toString());
    if(pic != null) {
      pic.setRenderMode(renderMode, comp);
    }
    repaint();
  }

  public RenderMode getRenderMode() {
    return renderMode;
  }

  @Override
  public void draw(final Graphics2D g) {
    switch(mode) {
      case 1:
        if(pic != null) {
          pic.draw(g);
        }
        break;
      case 2:
        if(img != null) {
          img.draw(g);
        }
        break;
      case 3:
        if(depth != null) {
          depth.draw(g);
        }
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

  public void setPicture(final File file)
      throws IOException {
    if(!isVisible()) {
      setVisible(true);
    }
    this.file = file;
    final BufferedImage i = ImageIO.read(file);
    final BufferedImage d = ImageIO.read(getDepth(file));
    img = new Picture(i);
    depth = new Picture(d);
    final Picture3D p = new Picture3D(img, depth);
    pic = null;
    comp.reset(new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight()));
    pic = p;
    pic.setRenderMode(renderMode, comp);
  }

  public void nextPicture() throws IOException {
    final File[] files = listImages(file.getParent());
    File cur = files[0];
    int i = files.length;
    while(--i >= 0) {
      if(files[i].equals(file)) {
        break;
      }
      cur = files[i];
    }
    setPicture(cur);
  }

  public void prevPicture() throws IOException {
    final File[] files = listImages(file.getParent());
    File cur = files[files.length - 1];
    for(final File f : files) {
      if(f.equals(file)) {
        break;
      }
      cur = f;
    }
    setPicture(cur);
  }

}
