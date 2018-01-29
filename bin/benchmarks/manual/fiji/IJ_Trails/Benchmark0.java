package manual.fiji.IJ_Trails;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class Benchmark0 {
  class Pixel {
    int frame;
    int pos;
    float val;
    public Pixel(int f, int p, float v) {
      frame = f;
      pos = p;
      val = v;
    }
  }

  public JavaPairRDD<Integer,Float> benchmark(JavaRDD<Pixel> tWinPix, int wcurr, int wmin, int wmax) {
    return tWinPix.mapToPair(pixel -> new Tuple2<Integer,Float>(pixel.pos, pixel.val))
            .aggregateByKey(
                    new Tuple2<Float,Integer>((float)0, 0),
                    (res, val) -> new Tuple2<Float,Integer>(res._1+val, res._2+1),
                    (res1, res2) -> new Tuple2<Float,Integer>(res1._1+res2._1, res1._2+res2._2)
            ).mapValues(pixel -> pixel._1/pixel._2);
  }
}
