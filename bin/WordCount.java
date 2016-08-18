import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Map;
import java.lang.Integer;
import java.lang.String;

public class WordCountJava {
	
	public static void main(String[] args) { countWords(null, null); }
	
	private static Map<String,Integer> countWords(List<String> words, Map<String,Integer> counts) {
		{
			int j = 0;
			j = 0;
			boolean loop$0 = false;
			loop$0 = false;
			SparkConf conf = new SparkConf().setAppName("spark");
			JavaSparkContext sc = new JavaSparkContext(conf);
			
			JavaRDD<java.lang.String> rdd_0_0 = sc.parallelize(words);
			
			final boolean loop0_final = loop$0;
			
			
			JavaPairRDD<Tuple2<Integer,String>, java.lang.Integer> mapEmits = rdd_0_0.flatMapToPair(new PairFlatMapFunction<java.lang.String, Tuple2<Integer,String>, java.lang.Integer>() {
				public Iterable<Tuple2<Tuple2<Integer,String>, java.lang.Integer>> call(java.lang.String words_j) throws Exception {
					List<Tuple2<Tuple2<Integer,String>, java.lang.Integer>> emits = new ArrayList<Tuple2<Tuple2<Integer,String>, java.lang.Integer>>();
					
					emits.add(new Tuple2(new Tuple2(0,words_j), 1));
					
					
					return emits;
				}
			});
			
			JavaPairRDD<Tuple2<Integer,String>, java.lang.Integer> reduceEmits = mapEmits.reduceByKey(new Function2<java.lang.Integer,java.lang.Integer,java.lang.Integer>(){
				public java.lang.Integer call(java.lang.Integer val1, java.lang.Integer val2) throws Exception {
					return val2 + val1;
				}
			});
			
			Map<Tuple2<Integer,String>, java.lang.Integer> output_rdd_0_0 = reduceEmits.collectAsMap();
			for(Tuple2<Integer,String> output_rdd_0_0_k : output_rdd_0_0.keySet()){
				counts.put(output_rdd_0_0_k._2, output_rdd_0_0.get(output_rdd_0_0_k));
			};
		}
		return counts;
	}
	
	public WordCountJava() { super(); }
}
