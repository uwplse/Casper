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
import java.lang.Integer;

public class Average {
	
	public static void main(String[] args) { avgList(null); }
	
	public static int avgList(List<Integer> data) {
		int sum = 0;
		sum = 0;
		int count = 0;
		count = 0;
		{
			int i = 0;
			i = 0;
			boolean loop$0 = false;
			loop$0 = false;
			SparkConf conf = new SparkConf().setAppName("spark");
			JavaSparkContext sc = new JavaSparkContext(conf);
			
			JavaRDD<java.lang.Integer> rdd_0_0 = sc.parallelize(data);
			
			final boolean loop0_final = loop$0;
			
			
			JavaPairRDD<Integer, Integer> mapEmits = rdd_0_0.flatMapToPair(new PairFlatMapFunction<java.lang.Integer, Integer, Integer>() {
				public Iterable<Tuple2<Integer, Integer>> call(java.lang.Integer data_i) throws Exception {
					List<Tuple2<Integer, Integer>> emits = new ArrayList<Tuple2<Integer, Integer>>();
					
					emits.add(new Tuple2(1,data_i));
					emits.add(new Tuple2(0,1));
					
					
					return emits;
				}
			});
			
			JavaPairRDD<Integer, Integer> reduceEmits = mapEmits.reduceByKey(new Function2<Integer,Integer,Integer>(){
				public Integer call(Integer val1, Integer val2) throws Exception {
					return val2 + val1;
				}
			});
			
			Map<Integer, Integer> output_rdd_0_0 = reduceEmits.collectAsMap();
			count = output_rdd_0_0.get(0);
			sum = output_rdd_0_0.get(1);
		}
		int flat$6 = sum / count;
		return flat$6;
	}
	
	public Average() { super(); }
}
