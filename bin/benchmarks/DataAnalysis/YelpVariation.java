package biglambda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YelpKids {
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
            if (record.goodForKids) {
                Integer prev = result.get(record.city);
                if (prev == null) {
                    prev = 0;
                }
                result.put(record.city, prev+1);
            }
        }

        return result;
    }
}