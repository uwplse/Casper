package magpie;

import java.util.List;

public class Convolve {

    /**
     * convolve
     *                        convolve data with a normalised gaussian in order to smooth the output after
     *                        reducing sample rate
     * @param data
     *                         2D data array to be smoothed
     * @param gaussian
     *                         normalised gaussian in the form of 1D array of doubles for y axis
     * @param numberOfSamples
     *                         sample number appropriate to size of original data array
     * @return
     *                        Smoothed data as array of doubles 
     */ 
    public static double[] convolve(int[] data, double[] gaussian, int numberOfSamples){
        double convolved[] = new double[data.length - (numberOfSamples + 1)];
        for (int i=0; i<convolved.length; i++){
            convolved[i] = 0.0;  // Set all doubles to 0.
            for (int j=i, k=0; j<i + numberOfSamples; j++, k++){
                convolved[i] +=  data[j] * gaussian[k];
            }
        }
        return convolved;
    }
}
