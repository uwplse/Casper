package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Count {
	public static int countList(JavaRDD<Integer> data){
	    return (int) data.count();
	}
}