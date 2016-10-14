import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;
import java.lang.String;

public class KMeansAdd {

	static Result calc_cov(Integer[][] matrix, int[] mean) {
        int i, j, k;
        int sum = 0;

        for (i = 0; i < num_rows; i++) {
            sum = 0;
            for (j = 0; j < num_cols; j++) {
                sum += matrix[i][j];
            }
            mean[i] = sum / num_cols;
        }
        
        return mean;
    }
    
}
