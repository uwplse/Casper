package generated.phoenix;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Histogram {
    public static class Pixel {
        public int r;
        public int g;
        public int b;
        
        public Pixel(int r, int g, int b) {
            super();
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
    
    public static int[][] histogram(JavaRDD<Pixel> rdd_0_0, int[] hR, int[] hG, int[] hB) {
        int i = 0;
        Map<Tuple2<Integer,Integer>,Integer> rdd_0_0_output = rdd_0_0.flatMapToPair(image_i -> {
            List<Tuple2<Tuple2<Integer,Integer>,Integer>> emits = new ArrayList<Tuple2<Tuple2<Integer,Integer>,Integer>>();
            emits.add(new Tuple2(new Tuple2(0, image_i.g),1));
            emits.add(new Tuple2(new Tuple2(1, image_i.b),1));
            emits.add(new Tuple2(new Tuple2(2, image_i.r),1));
            return emits.iterator();
        }).reduceByKey((v1,v2) -> v1 + v2).collectAsMap();
        for (Tuple2<Integer,Integer> rdd_0_0_output_k : rdd_0_0_output.keySet()) {
            if (rdd_0_0_output_k._1 == 0) {
                hG[rdd_0_0_output_k._2] = rdd_0_0_output.get(rdd_0_0_output_k);
            }
            if (rdd_0_0_output_k._1 == 1) {
                hB[rdd_0_0_output_k._2] = rdd_0_0_output.get(rdd_0_0_output_k);
            }
            if (rdd_0_0_output_k._1 == 2) {
                hR[rdd_0_0_output_k._2] = rdd_0_0_output.get(rdd_0_0_output_k);
            }
        }
        int[][] result = (int[][]) (new int[3][]);
        ((int[][]) result)[0] = (int[]) hR;
        ((int[][]) result)[1] = (int[]) hG;
        ((int[][]) result)[2] = (int[]) hB;
        return (int[][]) result;
    }
    
    public Histogram() { super(); }
}
