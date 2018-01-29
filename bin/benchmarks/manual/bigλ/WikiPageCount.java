package manual.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.Map;

public class WikiPageCount {
	class Record {
		public String name;
		public int views;
		public int something;
	}

	public Map<String,Integer> pageCount(JavaRDD<Record> data){
		return data.mapToPair(r -> new Tuple2<String, Integer>(r.name,r.views)).reduceByKey((a, b) -> a+b).collectAsMap();
	}
}