package biglambda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YelpScore {
    class Restaurant {
        public String state;
        public String city;
        public String comment;
        public int score;
        public Boolean goodForKids;
    }

    public Map<Integer,Integer> reviewCount(List<Restaurant> data, String keyState) {
        Map<Integer,Integer> result = new HashMap<Integer,Integer>();

        for (Restaurant record : data) {
            if (!result.containsKey(record.score)) {
                result.put(record.score, 0);
            }
            if (record.state.equals(keyState)) {
                result.put(record.score, result.get(record.score)+1);
            }
        }

        return result;
    }
}