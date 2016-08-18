import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.Map;
import java.lang.String;
import java.util.List;

public class StringMatch {
	
	public static void main(String[] args) { matchWords(null); }
	
	public static boolean[] matchWords(List<String> words) {
		String key1 = null;
		key1 = "key1";
		String key2 = null;
		key2 = "key2";
		boolean foundKey1 = false;
		foundKey1 = false;
		boolean foundKey2 = false;
		foundKey2 = false;
		{
			int i = 0;
			i = 0;
			boolean loop$0 = false;
			loop$0 = false;
			SparkConf conf = new SparkConf().setAppName("spark");
			JavaSparkContext sc = new JavaSparkContext(conf);
			
			JavaRDD<java.lang.String> rdd_0_0 = sc.parallelize(words);
			
			final java.lang.String key2_final = key2;
			final java.lang.String key1_final = key1;
			final boolean loop0_final = loop$0;
			
			
			JavaPairRDD<Integer, Boolean> mapEmits = rdd_0_0.flatMapToPair(new PairFlatMapFunction<java.lang.String, Integer, Boolean>() {
				public Iterable<Tuple2<Integer, Boolean>> call(java.lang.String words_i) throws Exception {
					List<Tuple2<Integer, Boolean>> emits = new ArrayList<Tuple2<Integer, Boolean>>();
					
					emits.add(new Tuple2(0,key2_final.equals(words_i)));
					emits.add(new Tuple2(1,words_i.equals(key1_final)));
					
					
					return emits;
				}
			});
			
			JavaPairRDD<Integer, Boolean> reduceEmits = mapEmits.reduceByKey(new Function2<Boolean,Boolean,Boolean>(){
				public Boolean call(Boolean val1, Boolean val2) throws Exception {
					return val1 || val2;
				}
			});
			
			Map<Integer, Boolean> output_rdd_0_0 = reduceEmits.collectAsMap();
			foundKey2 = output_rdd_0_0.get(0);
			foundKey1 = output_rdd_0_0.get(1);
		}
		boolean[] res = null;
		res = (new boolean[] { foundKey1, foundKey2 });
		return res;
	}
	
	public StringMatch() { super(); }
}
