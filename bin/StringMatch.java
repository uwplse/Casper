import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.Map;
import java.lang.String;
import java.util.List;

public class StringMatch {
	
	public static void main(String[] args) { matchWords(null); }
	
	public static boolean[] matchWords(List<String> words) {
		String key1 = null;
		key1 = "key1";
		String key2 = null;
		key2 = "key2";
		String key3 = null;
		key3 = "key3";
		boolean foundKey1 = false;
		foundKey1 = false;
		boolean foundKey2 = false;
		foundKey2 = false;
		boolean foundKey3 = false;
		foundKey3 = false;
		{
			int i = 0;
			i = 0;
			boolean loop$0 = false;
			loop$0 = false;
			while (true) {
				if (loop$0) {
					{
						int flat$1 = i + 1;
						i = (int) flat$1;
					}
					;
				}
				int flat$2 = words.size();
				loop$0 = i < flat$2;
				if (loop$0) {
					String flat$3 = words.get(i);
					boolean flat$4 = key1.equals(flat$3);
					if (flat$4) { foundKey1 = true; }
					String flat$5 = words.get(i);
					boolean flat$6 = key2.equals(flat$5);
					if (flat$6) { foundKey2 = true; }
					String flat$7 = words.get(i);
					boolean flat$8 = key3.equals(flat$7);
					if (flat$8) { foundKey3 = true; }
				} else {
					break;
				}
			}
		}
		boolean[] res = null;
		res = (new boolean[] { foundKey1, foundKey2, foundKey3 });
		return res;
	}
	
	public StringMatch() { super(); }
}
