import java.util.List;
import java.util.Arrays;
import java.lang.Integer;

public class Sum {	
	final float[] rmFirst(float[] tWinPix, int wmax) {
        for (int i=0; i < wmax; i++) {
            tWinPix[i] = tWinPix[i+1];
        }
        return tWinPix;
    }
}
