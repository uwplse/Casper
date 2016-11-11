package biglambda;

import java.util.ArrayList;
import java.util.List;

public class DatabaseSelect {
	
	class Table {
		public List<Record> records;
	}
	
	class Record {
	    public List<String> columns;
	}

	public List<Record> select(Table table, String key) {
        List<Record> result = new ArrayList<Record>();

        for (Record record : table.records) {
            if (record.columns.get(0).equals(key)) {
                result.add(record);
            }
        }
        
        return result;
    }
	
}
