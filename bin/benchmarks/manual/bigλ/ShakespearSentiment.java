package manual.bigλ;

import org.apache.spark.api.java.JavaRDD;

import java.util.HashMap;
import java.util.Map;

public class ShakespearSentiment {
	public Map<String,Integer> sentiment(JavaRDD<String> words){
    Map<String,Integer> result = new HashMap<String,Integer>();
    result.put("love", 0);
    result.put("hate", 0);

	  result = words.aggregate(
        result,
        (res, word) -> {
          if (word.trim().toLowerCase().equals("love")) {
            res.put("love", res.get("love")+1);
          }
          if (word.trim().toLowerCase().equals("hate")) {
            res.put("hate", res.get("hate")+1);
          }
          return res;
        },
        (res1, res2) -> {
          res1.put("love", res1.get("love") + res2.get("love"));
          res1.put("hate", res1.get("hate") + res2.get("hate"));
          return res1;
        }
    );

	  return result;
	}
}