public class ShiftLeft {	
	final float[] rmFirst(float[] tWinPix, int wmax) {
        for (int i=1; i < wmax; i++) {
            tWinPix[i-1] = tWinPix[i];
        }
        return tWinPix;
    }
}
