package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

import java.util.ArrayList;
import java.util.List;

public class ConditionalCount {
	public static Integer countList(JavaRDD<Integer> rdd_0_0){
		int count = 0;
		count = 0;
		count = rdd_0_0.flatMap(data_i -> {
			List<Integer> emits = new ArrayList<Integer>();
			if(data_i < 100) emits.add(1);
			return emits.iterator();
		}).reduce((val1, val2) -> (val1+val2));
		return count;
	}
}