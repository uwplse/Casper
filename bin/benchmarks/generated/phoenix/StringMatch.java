package generated.phoenix;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StringMatch {
    public static boolean[] matchWords(JavaRDD<String> rdd_0_0) {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        boolean foundKey1 = false;
        boolean foundKey2 = false;
        boolean foundKey3 = false;
        int i = 0;
        Map<Integer,Boolean> rdd_0_0_output = rdd_0_0.flatMapToPair(words_i -> {
            List<Tuple2<Integer,Boolean>> emits = new ArrayList<Tuple2<Integer,Boolean>>();
            if (words_i.equals(key3)) emits.add(new Tuple2(2,true));
            if (key1.equals(words_i)) emits.add(new Tuple2(3,true));
            if (key2.equals(words_i)) emits.add(new Tuple2(1,true));
            return emits.iterator();
        }).reduceByKey((v1,v2) -> v2 || v1).collectAsMap();
        if (rdd_0_0_output.containsKey(2)) foundKey3 = rdd_0_0_output.get(2);
        if (rdd_0_0_output.containsKey(3)) foundKey1 = rdd_0_0_output.get(3);
        if (rdd_0_0_output.containsKey(1)) foundKey2 = rdd_0_0_output.get(1);
        boolean[] res = { foundKey1, foundKey2, foundKey3 };
        return (boolean[]) res;
    }
    
    public StringMatch() { super(); }
}
