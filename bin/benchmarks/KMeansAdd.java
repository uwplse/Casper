import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;
import java.lang.String;

public class KMeansAdd {

	final float[] vsPixels(float[] pix) {
       for (int i = 0; i < pix.length; i++) {
            double raw = (double)pix[i];
            double transf = 2 * Math.sqrt(raw + 3 / 8);
            pix[i] = (float)transf;
        }
        return pix;
    }
}
