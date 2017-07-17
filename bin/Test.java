import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.lang.Integer;

public class ConditionalCount {
	
	public static void main(String[] args) { countList(null); }
	
	public static int countList(List<Integer> data) {
		int count = 0;
		count = 0;
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
				int flat$2 = data.size();
				loop$0 = i < flat$2;
				if (loop$0) {
					int var = 0;
					var = data.get(i);
					boolean flat$3 = var < 100;
					if (flat$3) {
						{
							int flat$4 = count + 1;
							count = (int) flat$4;
						}
						;
					}
				} else {
					break;
				}
			}
		}
		return count;
	}
	
	public ConditionalCount() { super(); }
}
