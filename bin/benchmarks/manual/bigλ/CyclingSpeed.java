package manual.bigλ;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.Map;

public class CyclingSpeed {
	class Record {
		public int fst;
		public int snd;
		public int emit;
		public double speed;
	}

	public Map<Integer,Integer> cyclingSpeed(JavaRDD<Record> data){
		return data.mapToPair(r -> new Tuple2<Integer,Integer>((int)Math.ceil(r.speed),1)).reduceByKey((a, b) -> a+b).collectAsMap();
	}
}