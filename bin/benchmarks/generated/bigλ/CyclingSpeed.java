package generated.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class CyclingSpeed {
	class Record {
		public int fst;
		public int snd;
		public int emit;
		public double speed;
	}

	public Map<Integer,Integer> cyclingSpeed(JavaRDD<Record> rdd_0_0){
    Map<Integer,Integer> result = null;
    result = new HashMap<Integer,Integer>();

    result = rdd_0_0.mapToPair(data_index -> new Tuple2<Integer,Integer>((int)Math.ceil(data_index.speed), 1)).reduceByKey((val1, val2) -> (val2+val1)).collectAsMap();

    return result;
	}
}
