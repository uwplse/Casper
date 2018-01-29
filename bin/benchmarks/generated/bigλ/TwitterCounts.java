package generated.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.*;

public class TwitterCounts {
  public Map<String, Integer> pairs(JavaRDD<String> rdd_0_0) {
    Map<String, Integer> result = null;
    result = new HashMap<String, Integer>();

    result = rdd_0_0.flatMap(tweets_index -> {
      List<String> emits = new ArrayList<String>();
      emits = Arrays.asList(tweets_index.split("\\s+"));
      return emits.iterator();
    }).flatMapToPair(val -> {
      List<Tuple2<String,Integer>> emits = new ArrayList<Tuple2<String,Integer>>();
      if('#' == val.charAt(0)) emits.add(new Tuple2(val, 1));
      return emits.iterator();
    }).reduceByKey((val1, val2) -> val1+val2).collectAsMap();

    return result;
  }
}
