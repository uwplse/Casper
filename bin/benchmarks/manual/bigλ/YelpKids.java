package manual.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.Map;

public class YelpKids {
	class Record {
		public String state;
		public String city;
		public String comment;
		public int score;
		public boolean goodForKids;
	}

	public Map<String,Integer> reviewCount(JavaRDD<Record> data){
		return data.mapToPair(r -> new Tuple2<String, Integer>(r.city,1)).reduceByKey((a, b) -> a+b).collectAsMap();
	}
}