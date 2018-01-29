package generated.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YelpKids {
	class Record {
		public String state;
		public String city;
		public String comment;
		public int score;
		public boolean goodForKids;
	}

	public Map<String,Integer> reviewCount(JavaRDD<Record> rdd_0_0){
    Map<String,Integer> result = null;
    result = new HashMap<String,Integer>();

	  result = rdd_0_0.flatMapToPair(data_index -> {
				List<Tuple2<String, java.lang.Integer>> emits = new ArrayList<Tuple2<String, java.lang.Integer>>();
				if(data_index.goodForKids) emits.add(new Tuple2(data_index.city, 1));
				return emits.iterator();
			}
		).reduceByKey((val1, val2) -> (val2+val1)).collectAsMap();
		
		return result;
	}
}