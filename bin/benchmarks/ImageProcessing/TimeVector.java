public class ShiftLeft {	
	/** Build time vector for this pixel for  given window. */
    final float[] getTvec(float[] tWinPix, int wmin, int wmax) {
        float[] tvec = new float[wmax - wmin + 1];
        for (int w = wmin; w < wmax; w++) {
            tvec[w] = tWinPix[w];  // time window vector for a pixel
        }
        return tvec;
    }
}
