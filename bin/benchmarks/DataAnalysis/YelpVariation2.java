package biglambda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YelpKids2 {
    class Restaurant {
        public String state;
        public String city;
        public String comment;
        public int score;
        public Boolean goodForKids;
    }

    public Map<String,Integer> reviewCount(List<Restaurant> data) {
        Map<String,Integer> result = new HashMap<String,Integer>();
        
        for (Restaurant record : data) {
            if (!result.containsKey(record.city)) {
                result.put(record.city, 0);
            }
            if (record.goodForKids) {
                result.put(record.city, result.get(record.city)+1);
            }
        }

        return result;
    }
}