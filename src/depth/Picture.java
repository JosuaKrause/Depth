package depth;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class Picture {

  public static final double MAX_COLOR = 255.0;

  public static final double MIN_COLOR = 0.0;

  private final WritableRaster out;

  protected final int width;

  protected final int height;

  private final BufferedImage img;

  public Picture(final BufferedImage img) {
    this.img = img;
    out = img.getRaster();
    width = out.getWidth();
    height = out.getHeight();
  }

  public boolean inRangeX(final int x) {
    return x >= 0 && x < width;
  }

  public boolean inRangeY(final int y) {
    return y >= 0 && y < height;
  }

  public double[] getPixel(final int x, final int y) {
    final double[] arr = new double[4];
    if(!inRangeX(x)) {
      return arr;
    }
    if(!inRangeY(y)) {
      return arr;
    }
    return out.getPixel(x, y, arr);
  }

  public void setPixel(final int x, final int y, final double[] pixel) {
    if(!inRangeX(x)) {
      return;
    }
    if(!inRangeY(y)) {
      return;
    }
    out.setPixel(x, y, pixel);
  }

  protected double clampColor(final double color) {
    return Math.max(MIN_COLOR, Math.min(MAX_COLOR, color));
  }

  public void draw(final Graphics g) {
    g.drawImage(img, 0, 0, null);
  }

  public double getDepth(final int x, final int y) {
    return getPixel(x, y)[0];
  }

  public void setDepth(final int x, final int y, final double depth) {
    final double[] pixel = { depth, depth, depth, MAX_COLOR};
    setPixel(x, y, pixel);
  }

  public void editDepth(final int xPos, final int yPos, final int radius,
      final double by, final Picture3D listener) {
    final double r2 = (double) radius * (double) radius;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for(int y = yPos - radius; y <= yPos + radius; ++y) {
      if(!inRangeY(y)) {
        continue;
      }
      for(int x = xPos - radius; x <= xPos + radius; ++x) {
        if(!inRangeX(x)) {
          continue;
        }
        final double diffX = xPos - x;
        final double diffY = yPos - y;
        if(diffX * diffX + diffY * diffY > r2) {
          continue;
        }
        final double d = getDepth(x, y);
        if(d < min) {
          min = d;
        }
        if(d > max) {
          max = d;
        }
      }
    }
    for(int y = yPos - radius; y <= yPos + radius; ++y) {
      if(!inRangeY(y)) {
        continue;
      }
      boolean chg = false;
      for(int x = xPos - radius; x <= xPos + radius; ++x) {
        if(!inRangeX(x)) {
          continue;
        }
        final double diffX = xPos - x;
        final double diffY = yPos - y;
        if(diffX * diffX + diffY * diffY > r2) {
          continue;
        }
        chg |= editPixel(x, y, by, by > 0 ? max : min, max == min);
      }
      if(chg && listener != null) {
        listener.updateLine(y);
      }
    }
  }

  private boolean editPixel(final int x, final int y, final double by,
      final double threshold, final boolean doThreshold) {
    final double d = getDepth(x, y);
    if(doThreshold) {
      if(by > 0) {
        if(d >= threshold) {
          return false;
        }
      } else {
        if(d <= threshold) {
          return false;
        }
      }
    }

    double nd = d + by;
    if(doThreshold) {
      if(by > 0) {
        if(nd > threshold) {
          nd = threshold;
        }
      } else {
        if(nd < threshold) {
          nd = threshold;
        }
      }
    }

    nd = clampColor(nd);

    if(d == nd) {
      return false;
    }
    setDepth(x, y, nd);
    return true;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

}
