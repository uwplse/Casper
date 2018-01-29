package manual.bigλ;

import org.apache.spark.api.java.JavaRDD;

import java.util.List;

public class DatabaseSelect {

	class Record {
		public List<String> columns;
	}

	public List<Record> select(JavaRDD<Record> table, String key){
		return table.filter(r -> r.columns.get(0).equals(key)).collect();
	}
}