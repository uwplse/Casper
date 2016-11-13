package biglambda;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

public class DatabaseSelect {
	
	class Record {
	    public List<String> columns;
	}

	public Set<Record> select(List<Record> records, String key) {
        Set<Record> result = new HashSet<Record>();

        for (Record record : records) {
            if (record.columns.get(0).equals(key)) {
                result.add(record);
            }
        }
        
        return result;
    }
	
}
