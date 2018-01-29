package manual.phoenix;

import org.apache.spark.api.java.JavaRDD;

public class LinearRegression {	

	public static class Point {
		public int x, y;
	
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static int[] regress(JavaRDD<Point> points) {
		return points.aggregate(new int[5],
														(res, point) -> {
															res[0] += point.x;
															res[1] += point.x * point.x;
															res[2] += point.y;
															res[3] += point.y * point.y;
															res[4] += point.x * point.y;
															return res;
														},
														(res1, res2) -> {
															res1[0] += res2[0];
															res1[1] += res2[1];
															res1[2] += res2[2];
															res1[3] += res2[3];
															res1[4] += res2[4];
															return res1;
														});
	}
	
}