package generated.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class WikiPageCount {
	class Record {
		public String name;
		public int views;
		public int something;
	}

	public Map<String,Integer> pageCount(JavaRDD<Record> rdd_0_0){
    Map<String,Integer> result = null;
    result = new HashMap<String,Integer>();

		result = rdd_0_0.mapToPair(data_index -> new Tuple2<String,Integer>(data_index.name,data_index.views)).reduceByKey((val1, val2) -> (val2+val1)).collectAsMap();
		
		return result;
	}
}