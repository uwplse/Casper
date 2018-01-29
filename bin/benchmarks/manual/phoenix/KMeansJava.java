package manual.phoenix;

import org.apache.spark.api.java.JavaDoubleRDD;
import org.apache.spark.api.java.JavaPairRDD;

/**
 * 
 * Translation of Phoenix k-means implementation
 * 
 */

public class KMeansJava {

	private static class Result{
		public Double[][] means;
		public int[] clusters;
		boolean modified;

		Result(Double[][] m, int[] c, boolean mod){
			this.means = m;
			this.clusters = c;
			this.modified = mod;
		}
	}

	final int GRID_SIZE = 1000;

	public static void main(String[] args) {

		int numPoints = 1000, numMeans = 10, dim = 3;

		Double[][] points = generatePoints(numPoints, dim);
		Double[][] means = generatePoints(numMeans, dim);
		int[] clusters = new int[numPoints];

		boolean modified = false;

		while (!modified) {
			modified = findClustersAndCalcMeans(points,means,
					clusters).modified;
		}

		System.out.println("\n\nFinal Means:\n");
		dumpMatrix(means);
	}

	private static void dumpMatrix(Double[][] a) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++)
				System.out.print(" " + a[i][j]);
			System.out.println();
		}
	}

	private static Result findClustersAndCalcMeans(Double[][] points,
			Double[][] means, int[] clusters) {
		int i, j;
		Double minDist, curDist;
		int minIdx;
		int dim = points[0].length;
		boolean modified = false;
		for (i = 0; i < points.length; i++) {
			minDist = getSqDist(points[i], means[0]);
			minIdx = 0;
			for (j = 1; j < means.length; j++) {
				curDist = getSqDist(points[i], means[j]);
				if (curDist < minDist) {
					minDist = curDist;
					minIdx = j;
				}
			}

			if (clusters[i] != minIdx) {
				clusters[i] = minIdx;
				modified = true;
			}
		}

		for (int ii = 0; ii < means.length; ii++) {
			Double[] sum = new Double[dim];
			int groupSize = 0;
			for (int jj = 0; jj < points.length; jj++) {
				if (clusters[jj] == ii) {
					sum = add(sum, points[jj]);
					groupSize++;
				}
			}
			dim = points[0].length;
			Double[] meansi = means[ii];
			for (int kk = 0; kk < dim; kk++) {
				if (groupSize != 0) {
					meansi[kk] = sum[kk] / groupSize;
				}
			}
			means[ii] = meansi;
		}
		return new Result(means, clusters, modified);
	}

	private static JavaDoubleRDD add(JavaDoubleRDD v1, JavaDoubleRDD v2) {
		JavaPairRDD<Double, Double> t = v1.zip(v2);
		return t.mapToDouble(val -> val._1 + val._2);
	}

	private static Double getSqDist(JavaDoubleRDD v1, JavaDoubleRDD v2) {
		JavaPairRDD<Double, Double> t = v1.zip(v2);
		return t.aggregate(0.0,
												(dist, val) -> dist + (val._1 - val._2) * (val._1 - val._2),
												(dist1, dist2) -> dist1 + dist2
											);
	}

	private static Double[][] generatePoints(int numPoints, int dim) {
		Double[][] p = new Double[numPoints][dim];
		for (int i = 0; i < numPoints; i++) {
			p[i] = new Double[dim];
			for (int j = 0; j < dim; j++)
				p[i][j] = Math.random();
		}
		return p;
	}

}