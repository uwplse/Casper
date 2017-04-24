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
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;
import java.lang.String;

public class WordCount {
	
	public static void main(String[] args) {
		List<String> words = null;
		words = Arrays.asList("foo", "bar", "cat", "bar", "dog");
		countWords(words);
	}
	
	private static Map<String,Integer> countWords(List<String> words) {
		Map<String,Integer> counts = null;
		counts = new HashMap<String,Integer>();
		{
			int j = 0;
			j = 0;
			boolean loop$0 = false;
			loop$0 = false;
			final boolean loop0_final = loop$0;
			
			JavaPairRDD<String, java.lang.Integer> mapEmits = rdd_0_0.flatMapToPair(new PairFlatMapFunction<java.lang.String, String, java.lang.Integer>() {
				public Iterator<Tuple2<String, java.lang.Integer>> call(java.lang.String words_j) throws Exception {
					List<Tuple2<String, java.lang.Integer>> emits = new ArrayList<Tuple2<String, java.lang.Integer>>();
					
					emits.add(new Tuple2(words_j, 1));
					
					
					return emits.iterator();
				}
			});
			
			JavaPairRDD<String, java.lang.Integer> reduceEmits = mapEmits.reduceByKey(new Function2<java.lang.Integer,java.lang.Integer,java.lang.Integer>(){
				public java.lang.Integer call(java.lang.Integer val1, java.lang.Integer val2) throws Exception {
					return (val2+val1);
				}
			});
			
			Map<String, java.lang.Integer> output_rdd_0_0 = reduceEmits.collectAsMap();
			counts = output_rdd_0_0;
			;
		}
		return counts;
	}
	
	public WordCount() { super(); }
}
