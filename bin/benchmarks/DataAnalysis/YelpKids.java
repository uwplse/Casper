package biglambda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YelpKids {
	class Record {
	    public String state;
	    public String city;
	    public String comment;
	    public int score;
	    public Map<String,Boolean> flags;
	}

	public Map<String,Integer> reviewCount(List<Record> data) {
        Map<String,Integer> result = new HashMap<String,Integer>();
        
        String key = "Good for Kids";
        
        for (Record record : data) {
            if (!result.containsKey(record.city)) {
                result.put(record.city, 0);
            }
            if (record.flags != null &&
                record.flags.get(key)) {
                result.put(record.city, result.get(record.city)+1);
            }
        }

        return result;
    }
}
