package depth;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.Comparator;

public class Picture3D extends Picture {

  private final Raster imgRaster;
  private final Raster depthRaster;

  public Picture3D(final BufferedImage img, final BufferedImage depth) {
    super(new BufferedImage(img.getWidth(), img.getHeight(),
        BufferedImage.TYPE_INT_RGB));
    imgRaster = img.getData();
    depthRaster = img.getData();
  }

  public void update() {
    updateLines(0, height - 1);
  }

  public void updateLines(final int lower, final int upper) {
    for(int y = lower; y <= upper; ++y) {
      if(!inRangeY(y)) {
        continue;
      }
      updateLine(y);
    }
  }

  private static final double FACTOR = MAX_COLOR / Math.log(MAX_COLOR);

  private double depth(final int x, final int y) {
    return Math.log(depthRaster.getPixel(x, y, new double[4])[0] + 1) * FACTOR;
  }

  private double[] origPixel(final int x, final int y) {
    return imgRaster.getPixel(x, y, new double[4]);
  }

  public void updateLine(final int y) {
    final double[] black = { 0, 0, 0, MAX_COLOR};
    final Integer[] perm = new Integer[width];
    for(int x = 0; x < width; ++x) {
      perm[x] = x;
      setPixel(x, y, black);
    }
    Arrays.sort(perm, new Comparator<Integer>() {

      @Override
      public int compare(final Integer i1, final Integer i2) {
        final double d1 = depth(i1, y);
        final double d2 = depth(i2, y);
        return Double.compare(d2, d1);
      }

    });
    for(int x = 0; x < width; ++x) {
      final int rx = perm[x];
      drawPixel(origPixel(rx, y), depth(rx, y), rx, y);
    }
  }

  public void setFactor(final double factor) {
    this.factor = factor / MAX_COLOR;
  }

  public double getFactor() {
    return factor * MAX_COLOR;
  }

  public void setBlur(final int blur) {
    this.blur = blur;
  }

  public int getBlur() {
    return blur;
  }

  protected double factor = 12.75 / MAX_COLOR;

  protected int blur = 4;

  private void drawPixel(final double[] pixel,
      final double depth, final int x, final int y) {
    final int dist = (int) (depth * factor);
    final int b = blur;
    for(int dx = 0; dx <= b; ++dx) {
      final double f = parts(dx, b);
      drawRed(pixel[0], x - dist - dx, y, f);
      drawCyan(pixel[1], pixel[2], x + dist - dx, y, f);
      if(dx != 0) {
        drawRed(pixel[0], x - dist + dx, y, f);
        drawCyan(pixel[1], pixel[2], x + dist + dx, y, f);
      }
    }
  }

  private static double parts(final double d, final double max) {
    return max > 0 ? 1.0 - d / max : 1.0;
  }

  private void drawRed(final double red, final int x, final int y,
      final double f) {
    final double[] pixel = getPixel(x, y);
    pixel[0] = clampColor(combine(pixel[0], red, f));
    setPixel(x, y, pixel);
  }

  private void drawCyan(final double green, final double blue, final int x,
      final int y, final double f) {
    final double[] pixel = getPixel(x, y);
    pixel[1] = clampColor(combine(pixel[1], green, f));
    pixel[2] = clampColor(combine(pixel[2], blue, f));
    setPixel(x, y, pixel);
  }

  private static double combine(final double orig, final double next,
      final double f) {
    return (1 - f) * orig + f * next;
    // return Math.max(orig, f * next);
  }

}
