package generated.phoenix;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class WordCount {
    private static Map countWords(JavaRDD<String> rdd_0_0) {
        Map counts = new HashMap();
        int j = 0;
        counts = rdd_0_0.mapToPair(words_i -> new Tuple2<String,Integer>(words_i, 1)).reduceByKey((v1,v2) -> v2 + v1).collectAsMap();
        return (Map) counts;
    }
    
    public WordCount() { super(); }
}
