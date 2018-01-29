package generated.stats;

import java.io.PrintWriter;

public class PlotWriter {
  public static void write(double[] x, double[] y, PrintWriter fileOut) {
    int i = 0;
    while (i < ((double[]) y).length) {
      ((PrintWriter) fileOut).printf(
              "%2.5f %2.5f",
              (Object[])
                      (new Object[] { Double.valueOf(
                              ((double[])
                                      x)[i]),
                              Double.valueOf(((double[]) y)[i]) }));
      ((PrintWriter) fileOut).println();
      i++;
    }
  }

  public static void write(double[][] data, PrintWriter fileOut) {
    int i = 0;
    while (i < ((double[][]) data).length) {
      ((PrintWriter) fileOut).
              printf("%2.5f %2.5f",
                      (Object[])
                              (new Object[] { Double.valueOf(((double[])
                                      ((double[][])
                                              data)[i])[0]),
                                      Double.valueOf(((double[])
                                              ((double[][]) data)[i])[1]) }));
      ((PrintWriter) fileOut).println();
      i++;
    }
  }

  public static void errors(double[] x, double[] y, double xError,
                            double[] yError, PrintWriter fileOut) {
    int i = 0;
    while (i < ((double[]) x).length) {
      ((PrintWriter) fileOut).printf(
              "%2.5f %2.5f %2.5f %2.5f ",
              (Object[])
                      (new Object[] { Double.valueOf(
                              ((double[])
                                      x)[i]),
                              Double.valueOf(((double[]) y)[i]),
                              Double.valueOf(xError),
                              Double.valueOf(((double[])
                                      yError)[i]) }));
      ((PrintWriter) fileOut).println();
      i++;
    }
  }

  public static void errorsFit(double[] x, double[] y, double[] fit,
                               double xError, double[] yError,
                               PrintWriter fileOut) {
    int i = 0;
    while (i < ((double[]) y).length) {
      ((PrintWriter) fileOut).printf(
              "%2.5f %2.5f %2.5f %2.5f %2.5f",
              (Object[])
                      (new Object[] { Double.valueOf(
                              ((double[])
                                      x)[i]),
                              Double.valueOf(((double[]) y)[i]),
                              Double.valueOf(((double[]) fit)[i]),
                              Double.valueOf(xError),
                              Double.valueOf(((double[])
                                      yError)[i]) }));
      ((PrintWriter) fileOut).println();
      i++;
    }
  }

  public static void aColumn(double[] n, PrintWriter fileOut) {
    int i = 0;
    while (i < ((double[]) n).length) {
      ((PrintWriter) fileOut).printf(
              "%2.5f ",
              (Object[])
                      (new Object[] { Double.valueOf(
                              ((double[])
                                      n)[i]) }));
      ((PrintWriter) fileOut).println();
      i++;
    }
  }

  public static void calibrate(double[] x, double[] y, PrintWriter fileOut,
                               double[] amount) {
    double[] calibrate = (double[]) (new double[((double[]) x).length]);
    int i = 0;
    while (i < ((double[]) y).length) {
      System.out.println(((double[]) amount)[i]);
      ((double[]) calibrate)[i] += ((double[]) x)[i] -
              ((double[]) amount)[i];
      ((PrintWriter) fileOut).
              printf("%2.2f %2.2f",
                      (Object[])
                              (new Object[] { Double.valueOf(((double[])
                                      calibrate)[i]),
                                      Double.valueOf(((double[]) y)[i]) }));
      ((PrintWriter) fileOut).println();
      i++;
    }
  }

  public static void peaksUp(double[][] data, PrintWriter upFile,
                             double minValue) {
    double largeUp = 0.0;
    int i = 0;
    while (i < ((double[][]) data).length - 1) {
      if (((double[]) ((double[][]) data)[i])[1] >
              ((double[]) ((double[][]) data)[0])[1]) {
        if (((double[]) ((double[][]) data)[i])[1] > largeUp &&
                ((double[]) ((double[][]) data)[i])[1] > minValue) {
          largeUp = ((double[]) ((double[][]) data)[i])[1];
          System.out.
                  printf(
                          "%2.2f ",
                          (Object[])
                                  (new Object[] { Double.valueOf(
                                          ((double[])
                                                  ((double[][])
                                                          data)[i])[0]) }));
          ((PrintWriter) upFile).
                  printf(
                          "%2.2f %2.2f ",
                          (Object[])
                                  (new Object[] { Double.valueOf(((double[])
                                          ((double[][])
                                                  data)[i])[0]),
                                          Double.valueOf(largeUp) }));
          ((PrintWriter) upFile).println();
        }
      }
      i++;
    }
    System.out.println();
  }

  public PlotWriter() { super(); }
}