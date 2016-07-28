import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.lang.Integer;

public class SummationArray {
    
    public static void main(String[] args) { sumList(null); }
    
    public static int sumList(List<Integer> data) {
        int sum = 0;
        sum = 0;
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
                    Integer flat$3 = data.get(i);
                    int flat$4 = sum + flat$3;
                    sum = (int) flat$4;
                } else {
                    break;
                }
            }
        }
        return sum;
    }
    
    public SummationArray() { super(); }
}
