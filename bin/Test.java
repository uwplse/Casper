import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Sum {
	
	public static void main(String[] args) {
		List<Integer> numbers = null;
		numbers = Arrays.asList(10, 5, 7, 12, 3);
		sumList(numbers);
	}
	
	public static int sumList(List<Integer> data) {
		int sum = 0;
		sum = 0;
		for (Integer val : data) {
			int flat$0 = sum + val;
			sum = (int) flat$0;
		}
		return sum;
	}
	
	public Sum() { super(); }
}
