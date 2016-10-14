import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;
import java.lang.String;

public class KMeansAdd {

	final float calcSD(List<Float> vec) {
        float sd = 0;
        float mean = 0;
        float variance = 0;
        for (Float v : vec) {
            mean += v;
        }
        mean /= vec.size();
        for (Float v: vec) {
            variance += (mean - v) * (mean - v);
        }
        variance /= vec.size();
        sd = (float)Math.sqrt(variance);
        return sd;
    }
    
}
