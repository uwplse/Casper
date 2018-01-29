package generated.bigλ;

import org.apache.spark.api.java.JavaRDD;

import java.util.ArrayList;
import java.util.List;

public class DatabaseSelect {

	class Record {
		public List<String> columns;
	}

	public List<Record> select(JavaRDD<Record> rdd_0_0, String key){
    List<Record> result = null;
    result = new ArrayList<Record>();

    final String key_final = key;
		result = rdd_0_0.flatMap(data_index -> {
      List<Record> emits = new ArrayList<Record>();
      if(data_index.columns.get(0).equals(key_final)) emits.add(data_index);
      return emits.iterator();
    }).collect();

		return result;
	}
}