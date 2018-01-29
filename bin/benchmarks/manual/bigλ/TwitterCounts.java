package manual.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TwitterCounts {
  public Map<String, Integer> pairs(JavaRDD<String> tweets) {
    return tweets.flatMapToPair(tweet -> {
      List<Tuple2<String,Integer>> freq = new ArrayList<Tuple2<String,Integer>>();
      for (String word : tweet.split("\\s+")) {
        if (word.charAt(0) == '#') {
          freq.add(new Tuple2(word, 1));
        }
      }
      return freq.iterator();
    }).reduceByKey((a, b) -> a+b).collectAsMap();
  }
}
