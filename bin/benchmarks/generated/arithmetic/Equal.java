package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

import java.util.ArrayList;
import java.util.List;

public class Equal {
	public static boolean equal(JavaRDD<Integer> rdd_0_0, int val){
		boolean equal = false;
		equal = true;
		final int val_final = val;
		equal = rdd_0_0.flatMap(data_i -> {
			List<Boolean> emits = new ArrayList<Boolean>();
			if(val_final != data_i) emits.add(false);
			return emits.iterator();
		}).reduce((val1, val2) -> false);
		return equal;
	}
}