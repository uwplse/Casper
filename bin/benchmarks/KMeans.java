import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Sum {	
	
	/*private static Double[] add(List<Double> v1, List<Double> v2) {
		Double[] sum = new Double[v1.size()];
		for (int i = 0; i < v1.size(); i++)
			sum[i] = v1.get(i) + v2.get(i);
		return sum;
	}*/
	
	private static Double getSqDist(List<Double> v1, List<Double> v2) {
		Double dist = 0.0;
		for (int i = 0; i < v1.size(); i++)
			dist += ((v1.get(i) - v2.get(i)) * (v1.get(i) - v2.get(i)));
		return dist;
	}
	
}
