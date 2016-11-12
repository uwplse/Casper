package biglambda;

import java.util.HashMap;
import java.util.Map;

public class ShakespearSentiment {   
    public Map<String,Integer> sentiment(String text) {
        Map<String,Integer> result = new HashMap<String,Integer>();

        result.put("love", 0);
        result.put("hate", 0);
        
        for (String word : text.split(" ")) {
            if (word.equals("love")) {
                result.put("love", result.get("love")+1);
            }
            else if (word.equals("hate")) {
                result.put("hate", result.get("hate")+1);
            }
        }

        return result;
    }
}
