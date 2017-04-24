package magpie;

import java.util.List;

public class MulVecVec {
    public static double[] multiply(double[] a, double[]b){
        double[] temp= new double[a.length];
        for(int i=0; i<a.length;i++) temp[i]= a[i] * b[i];
        return temp;
    }
}
