package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Min {
  public static int minList(JavaRDD<Integer> data){
    return data.reduce((a, b) -> Math.min(a, b));
	}
}