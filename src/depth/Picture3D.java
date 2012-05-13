package depth;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.Comparator;

public class Picture3D extends Picture {

  public static enum RenderMode {

    SORTED_BLUR_LOG(4, false, true, true),

    DEPTH_MEM_LOG(0, true, false, true),

    SORTED_BLUR(4, false, true, false),

    DEPTH_MEM(0, true, false, false),

    ;

    public final int blur;

    public final boolean depthMem;

    public final boolean sort;

    public final boolean logScale;

    private RenderMode(final int blur, final boolean depthMem,
        final boolean sort, final boolean logScale) {
      this.blur = blur;
      this.depthMem = depthMem;
      this.sort = sort;
      this.logScale = logScale;
    }
  }

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
    final double depth = depthRaster.getPixel(x, y, new double[4])[0];
    if(renderMode.logScale) {
      return Math.log(depth + 1) * FACTOR;
    }
    return depth;
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
    if(renderMode.sort) {
      Arrays.sort(perm, new Comparator<Integer>() {

        @Override
        public int compare(final Integer i1, final Integer i2) {
          final double d1 = depth(i1, y);
          final double d2 = depth(i2, y);
          return Double.compare(d2, d1);
        }

      });
    }
    final double[] redDepth = new double[width];
    final double[] cyanDepth = new double[width];
    for(int x = 0; x < width; ++x) {
      final int rx = perm[x];
      drawPixel(origPixel(rx, y), depth(rx, y), rx, y, redDepth, cyanDepth);
    }
  }

  public void setFactor(final double factor) {
    this.factor = factor / MAX_COLOR;
  }

  public double getFactor() {
    return factor * MAX_COLOR;
  }

  public void setRenderMode(final RenderMode renderMode) {
    this.renderMode = renderMode;
    update();
  }

  public RenderMode getRenderMode() {
    return renderMode;
  }

  private RenderMode renderMode = RenderMode.SORTED_BLUR_LOG;

  protected double factor = 12.75 / MAX_COLOR;

  private void drawPixel(final double[] pixel,
      final double depth, final int x, final int y, final double[] redDepth,
      final double[] cyanDepth) {
    final int dist = (int) (depth * factor);
    final int b = renderMode.blur;
    for(int dx = 0; dx <= b; ++dx) {
      final double f = parts(dx, b);
      drawRed(pixel[0], x - dist - dx, y, f, redDepth, depth);
      drawCyan(pixel[1], pixel[2], x + dist - dx, y, f, cyanDepth, depth);
      if(dx != 0) {
        drawRed(pixel[0], x - dist + dx, y, f, redDepth, depth);
        drawCyan(pixel[1], pixel[2], x + dist + dx, y, f, cyanDepth, depth);
      }
    }
  }

  private static double parts(final double d, final double max) {
    return max > 0 ? 1.0 - d / max : 1.0;
  }

  private void drawRed(final double red, final int x, final int y,
      final double f, final double[] redDepth, final double depth) {
    final double[] pixel = getPixel(x, y);
    if(!inRangeX(x)) {
      return;
    }
    if(renderMode.depthMem && -depth > redDepth[x]) {
      return;
    }
    pixel[0] = clampColor(combine(pixel[0], red, f));
    setPixel(x, y, pixel);
    redDepth[x] = -depth;
  }

  private void
      drawCyan(final double green, final double blue, final int x,
          final int y, final double f, final double[] cyanDepth,
          final double depth) {
    final double[] pixel = getPixel(x, y);
    if(!inRangeX(x)) {
      return;
    }
    if(renderMode.depthMem && -depth > cyanDepth[x]) {
      return;
    }
    pixel[1] = clampColor(combine(pixel[1], green, f));
    pixel[2] = clampColor(combine(pixel[2], blue, f));
    setPixel(x, y, pixel);
    cyanDepth[x] = -depth;
  }

  private static double combine(final double orig, final double next,
      final double f) {
    return (1 - f) * orig + f * next;
    // return Math.max(orig, f * next);
  }

}
