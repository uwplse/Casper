package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

import java.util.ArrayList;
import java.util.List;

public class ConditionalSum {
	public static Integer sumList(JavaRDD<Integer> rdd_0_0){
		int sum = 0;
		sum = 0;
		sum = rdd_0_0.flatMap(data_i -> {
			List<Integer> emits = new ArrayList<Integer>();
			if(data_i < 100) emits.add(data_i);
			return emits.iterator();
		}).reduce((val1, val2) -> (val1+val2));
		return sum;
	}
}