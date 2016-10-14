import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;
import java.lang.String;

public class KMeansAdd {

	static void calc_cov(List<Integer> matrix, int rowSize, int colSize, List<Integer> mean) {
		
        for (int i = 0; i < matrix.size(); i++) {
            int row = i / colSize;
            int value = matrix.get(i) +  mean.get(row);
            mean.set(row, value);
        }
        
        for (int i = 0; i < mean.size(); i++) {
        	int value = mean.get(i) / colSize;
			mean.set(i, value);
        }
    }
    
}
