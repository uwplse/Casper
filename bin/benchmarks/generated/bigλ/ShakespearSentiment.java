package generated.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShakespearSentiment {
	public Map<String,Integer> sentiment(JavaRDD<String> rdd_0_0){
		Map<String,Integer> result = null;
		result = new HashMap<String,Integer>();

		result.put("love", 0);
		result.put("hate", 0);
		
		result = rdd_0_0.flatMapToPair(words_index -> {
      List<Tuple2<String, java.lang.Integer>> emits = new ArrayList<Tuple2<String, java.lang.Integer>>();
      if("hate".equals(words_index.trim().toLowerCase())) emits.add(new Tuple2(words_index, 1));
      if(words_index.trim().toLowerCase().equals("love")) emits.add(new Tuple2(words_index, 1));
      return emits.iterator();
    }).reduceByKey((val1, val2) -> (val2+val1)).collectAsMap();

		return result;
	}
}