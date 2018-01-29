package generated.fiji.IJ_Trails;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;
import scala.Tuple3;

import java.util.ArrayList;
import java.util.List;

/**
 * Trails_.java: line 151
 */
public class Benchmark1 {
  public JavaPairRDD<Integer, Float> benchmark(JavaRDD<Tuple3<Integer, Integer, Float>> rdd_0_0, int v, int wmin, int wmax) {
    return rdd_0_0.flatMapToPair(tWinPix_i -> {
      List<Tuple2<Integer, Float>> emits = new ArrayList<>();
      if (v == tWinPix_i._2()) new Tuple2(tWinPix_i._1(), tWinPix_i._3());
      return emits.iterator();
    });
  }
}