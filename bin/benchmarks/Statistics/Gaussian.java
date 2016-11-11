package magpie;

import java.util.List;

public class Gaussian {
	
	private static final double period = 2 * Math.PI;
	
    /**
    * gaussian
    *                         a normalised gaussian to reflect the distribution of the data 
    * @param numberOfSamples
    *                         sample number appropriate to size of original data array
    * @param variance                    
    *                         variance of data
    * @param mean
    *                         mean of data
    * @return
    *                         normalised gaussian in the form of 1D array of doubles for y axis
    */
    public static double[] gaussian(int numberOfSamples, double variance, double mean){
        double[] gaussian = new double[numberOfSamples];
        double tempGaussian= 0.0;

        for (int i=0; i<numberOfSamples; i++){
            gaussian[i] = Math.sqrt(1/(period)* variance)*(Math.exp(-(i-mean)*(i-mean)/(2 * variance)));
            tempGaussian += gaussian[i];
        }
        
        for (int i=0; i< numberOfSamples; i++){ //normalise
            gaussian[i] /= tempGaussian;
        }
        return gaussian;
    }
}
