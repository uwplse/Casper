package manual.phoenix;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.Map;

public class WordCount {
	private static Map<String, Integer> countWords(JavaRDD<String> words) {
		return words.mapToPair(word -> new Tuple2<String, Integer>(word,1)).reduceByKey((c1, c2) -> c1+c2).collectAsMap();
	}
}