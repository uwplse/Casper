package generated.stats;

import java.util.Scanner;

public class PlotReader {
    public static double[] data1Column(Scanner scan, int length) {
        double[] data = (double[]) (new double[length]);
        int i = 0;
        while (i < ((double[]) data).length) {
            ((double[]) data)[i] = IOUtil.skipToDouble((Scanner) scan);
            i++;
        }
        return (double[]) data;
    }

    public static double[][] data2Column(Scanner scan, int length) {
        double[][] data = (double[][]) (new double[length][2]);
        int i = 0;
        while (i < ((double[][]) data).length) {
            int j = 0;
            while (j < ((double[]) ((double[][]) data)[0]).length) {
                ((double[]) ((double[][]) data)[i])[j] =
                        (float) IOUtil.skipToDouble((Scanner) scan);
                j++;
            }
            i++;
        }
        return (double[][]) data;
    }

    public static double[][] data3Column(Scanner scan, int length) {
        double[][] data = (double[][]) (new double[length][3]);
        int i = 0;
        while (i < ((double[][]) data).length) {
            int j = 0;
            while (j < ((double[]) ((double[][]) data)[0]).length) {
                ((double[]) ((double[][]) data)[i])[j] =
                        (float) IOUtil.skipToDouble((Scanner) scan);
                j++;
            }
            i++;
        }
        return (double[][]) data;
    }

    public static double[][] data4Column(Scanner scan, int length) {
        double[][] data = (double[][]) (new double[length][4]);
        int i = 0;
        while (i < ((double[][]) data).length) {
            int j = 0;
            while (j < ((double[]) ((double[][]) data)[0]).length) {
                ((double[]) ((double[][]) data)[i])[j] =
                        (float) IOUtil.skipToDouble((Scanner) scan);
                j++;
            }
            i++;
        }
        return (double[][]) data;
    }

    public PlotReader() { super(); }
}