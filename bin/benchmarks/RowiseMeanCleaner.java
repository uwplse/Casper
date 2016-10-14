import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;
import java.lang.String;

public class KMeansAdd {

	static Result calc_cov(Integer[][] matrix, int[] mean) {

        for (int i = 0; i < num_rows; i++) {
            int sum = 0;
            for (int j = 0; j < num_cols; j++) {
                sum += matrix[i][j];
            }
            mean[i] = sum / num_cols;
        }
        
        return mean;
    }
    
}
