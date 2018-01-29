package generated.fiji.IJ_Trails;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;
import scala.Tuple3;

public class Benchmark0 {
  public JavaPairRDD<Integer,Float> benchmark(JavaRDD<Tuple3<Integer,Integer,Float>> rdd_0_0, int wcurr, int wmin, int wmax) {
    return rdd_0_0.mapToPair(tWinPix_i -> new Tuple2<Integer,Tuple2<Integer,Float>>(tWinPix_i._2(), new Tuple2<>(1, tWinPix_i._3()))).reduceByKey((v1,v2) -> new Tuple2<Integer,Float>(v1._1+v2._1,v1._2+v2._2)).mapValues(v -> v._2/v._1);
  }
}
