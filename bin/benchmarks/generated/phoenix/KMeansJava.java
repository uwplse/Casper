package generated.phoenix;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

/**
 * 
 * Translation of Phoenix k-means implementation
 * 
 */
public class KMeansJava {
    private static class Result {
        public Double[][] means;
        public int[] clusters;
        boolean modified;
        
        Result(Double[][] m, int[] c, boolean mod) {
            super();
            this.means = (Double[][]) m;
            this.clusters = (int[]) c;
            this.modified = mod;
        }
    }
    
    final int GRID_SIZE = 1000;
    
    public static void main(String[] args) {
        int numPoints = 1000;
        int numMeans = 10;
        int dim = 3;
        Double[][] points = generatePoints(numPoints, dim);
        Double[][] means = generatePoints(numMeans, dim);
        int[] clusters = (int[]) (new int[numPoints]);
        boolean modified = false;
        while (!modified) {
            modified = findClustersAndCalcMeans((Double[][]) points,
                                                (Double[][]) means,
                                                (int[]) clusters).modified;
        }
        System.out.println("\n\nFinal Means:\n");
        dumpMatrix((Double[][]) means);
    }
    
    private static void dumpMatrix(Double[][] a) {
        int i = 0;
        while (i < ((Double[][]) a).length) {
            int j = 0;
            while (j < ((Double[]) ((Double[][]) a)[i]).length) {
                System.out.print((String)
                                   (" " + ((Double[]) ((Double[][]) a)[i])[j]));
                j++;
            }
            System.out.println();
            i++;
        }
    }
    
    private static Result findClustersAndCalcMeans(Double[][] points,
                                                   Double[][] means,
                                                   int[] clusters) {
        int i;
        int j;
        Double minDist;
        Double curDist;
        int minIdx;
        int dim = ((Double[]) ((Double[][]) points)[0]).length;
        boolean modified = false;
        i = 0;
        while (i < ((Double[][]) points).length) {
            minDist = getSqDist((Double[]) ((Double[][]) points)[i],
                                (Double[]) ((Double[][]) means)[0]);
            minIdx = 0;
            j = 1;
            while (j < ((Double[][]) means).length) {
                curDist = getSqDist((Double[]) ((Double[][]) points)[i],
                                    (Double[]) ((Double[][]) means)[j]);
                if (((Double) curDist).doubleValue() <
                      ((Double) minDist).doubleValue()) {
                    minDist = (Double) curDist;
                    minIdx = j;
                }
                j++;
            }
            if (((int[]) clusters)[i] != minIdx) {
                ((int[]) clusters)[i] = minIdx;
                modified = true;
            }
            i++;
        }
        int ii = 0;
        while (ii < ((Double[][]) means).length) {
            Double[] sum = (Double[]) (new Double[dim]);
            int groupSize = 0;
            int jj = 0;
            while (jj < ((Double[][]) points).length) {
                if (((int[]) clusters)[jj] == ii) {
                    sum = add((Double[]) sum,
                              (Double[]) ((Double[][]) points)[jj]);
                    groupSize++;
                }
                jj++;
            }
            dim = ((Double[]) ((Double[][]) points)[0]).length;
            Double[] meansi = (Double[]) ((Double[][]) means)[ii];
            int kk = 0;
            while (kk < dim) {
                if (groupSize != 0) {
                    ((Double[]) meansi)[kk] =
                      Double.valueOf(((Double)
                                        ((Double[]) sum)[kk]).doubleValue() /
                                         groupSize);
                }
                kk++;
            }
            ((Double[][]) means)[ii] = (Double[]) meansi;
            ii++;
        }
        return new Result((Double[][]) means, (int[]) clusters, modified);
    }
    
    private static JavaRDD<Double> add(JavaRDD<Double> rdd_0_0, JavaRDD<Double> rdd_0_1) {
        JavaPairRDD<Double,Double> rdd_0_2 = rdd_0_0.zip(rdd_0_1);
        return rdd_0_2.map(casper_dataset_i -> casper_dataset_i._1 + casper_dataset_i._2);
    }
    
    private static Double getSqDist(JavaRDD<Double> rdd_1_0, JavaRDD<Double> rdd_1_1) {
        Double dist = Double.valueOf(0.0);
        int i = 0;
        JavaPairRDD<Double,Double> rdd_1_2 = rdd_1_0.zip(rdd_1_1);
        dist = rdd_1_2.map(casper_dataset_i -> (casper_dataset_i._2 - casper_dataset_i._1) * (casper_dataset_i._2 - casper_dataset_i._1)).reduce((v1,v2) -> v1 + v2);
        return (Double) dist;
    }
    
    private static Double[][] generatePoints(int numPoints, int dim) {
        Double[][] p = (Double[][]) (new Double[numPoints][dim]);
        int i = 0;
        while (i < numPoints) {
            ((Double[][]) p)[i] = (Double[]) (new Double[dim]);
            int j = 0;
            while (j < dim) {
                ((Double[]) ((Double[][]) p)[i])[j] =
                  Double.valueOf(Math.random());
                j++;
            }
            i++;
        }
        return (Double[][]) p;
    }
    
    public KMeansJava() { super(); }
}
