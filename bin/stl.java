package magpie;

import java.util.List;

public class StatsUtilList {
	private static final double period = 2 * Math.PI;
	
	public static double regressionSumOfSquares(List<Double> fit, double yMean) {
		double ssr = 0;
		ssr = 0.0;
		{
			int i = 0;
			i = 0;
			boolean loop$0 = false;
			loop$0 = false;
			while (true) {
				if (loop$0) {
					{
						int flat$1 = i + 1;
						i = (int) flat$1;
					}
					;
				}
				int flat$2 = fit.size();
				loop$0 = i < flat$2;
				if (loop$0) {
					Double flat$3 = fit.get(i);
					double flat$4 = flat$3 - yMean;
					Double flat$5 = fit.get(i);
					double flat$6 = flat$5 - yMean;
					double flat$7 = flat$4 * flat$6;
					double flat$8 = ssr + flat$7;
					ssr = (double) flat$8;
				} else {
					break;
				}
			}
		}
		return ssr;
	}
	
	public StatsUtilList() { super(); }
}
