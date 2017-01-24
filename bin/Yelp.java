package biglambda;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YelpKids2 {
	class Restaurant {
		public String state;
		public String city;
		public String comment;
		public int score;
		public Boolean goodForKids;
		
		public Restaurant() { super(); }
	}
	
	
	public Map<String,Integer> reviewCount(List<Restaurant> data) {
		Map<String,Integer> result = null;
		result = new HashMap<String,Integer>();
		SparkConf conf = new SparkConf().setAppName("spark");
		JavaSparkContext sc = new JavaSparkContext(conf);
		
		JavaRDD<biglambda.YelpKids2.Restaurant> rdd_0_0 = sc.parallelize(data);
		
		JavaPairRDD<Tuple2<Integer,String>, Tuple2<Integer,java.lang.Integer>> mapEmits = rdd_0_0.flatMapToPair(new PairFlatMapFunction<biglambda.YelpKids2.Restaurant, Tuple2<Integer,String>, Tuple2<Integer,java.lang.Integer>>() {
			public Iterator<Tuple2<Tuple2<Integer,String>, Tuple2<Integer,java.lang.Integer>>> call(biglambda.YelpKids2.Restaurant data_casper_index) throws Exception {
				List<Tuple2<Tuple2<Integer,String>, Tuple2<Integer,java.lang.Integer>>> emits = new ArrayList<Tuple2<Tuple2<Integer,String>, Tuple2<Integer,java.lang.Integer>>>();
				
				if(data_casper_index.goodForKids) emits.add(new Tuple2(new Tuple2(1,data_casper_index.city), 1));
				
				
				return emits.iterator();
			}
		});
		
		JavaPairRDD<Tuple2<Integer,String>, Tuple2<Integer,java.lang.Integer>> reduceEmits = mapEmits.reduceByKey(new Function2<Tuple2<Integer,java.lang.Integer>,Tuple2<Integer,java.lang.Integer>,Tuple2<Integer,java.lang.Integer>>(){
			public Tuple2<Integer,java.lang.Integer> call(Tuple2<Integer,java.lang.Integer> val1, Tuple2<Integer,java.lang.Integer> val2) throws Exception {
				if(val1._1 == 0){
					return new Tuple2(val1._1,(val2._2+val1._2));
				}
				
				return null;
			}
		});
		
		Map<Tuple2<Integer,String>, Tuple2<Integer,java.lang.Integer>> output_rdd_0_0 = reduceEmits.collectAsMap();
		for(Tuple2<Integer,String> output_rdd_0_0_k : output_rdd_0_0.keySet()){
			if(output_rdd_0_0_k._1 == 0) result.put(output_rdd_0_0_k._2, output_rdd_0_0.get(output_rdd_0_0_k)._2);
		};;
		return result;
	}
	
	public YelpKids2() { super(); }
}
