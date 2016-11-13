package biglambda;

import java.util.HashMap;
import java.util.Map;

public class ShakespearSentiment {   
    public Map<String,Integer> sentiment(String text) {
        Map<String,Integer> result = new HashMap<String,Integer>();

		String keyword1 = "love";
		String keyword2 = "hate";
		
        result.put(keyword1, 0);
        result.put(keyword2, 0);
        
        String[] words = text.split(" ");
        
        for (String word : words) {
            if (word.equals(keyword1)) {
                result.put(keyword1, result.get(keyword1)+1);
            }
            else if (word.equals(keyword2)) {
                result.put(keyword2, result.get(keyword2)+1);
            }
        }

        return result;
    }
}
