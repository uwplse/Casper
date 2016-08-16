import java.util.List;
import java.lang.Integer;

public class Equal {	
	public static void main(String[] args) {
		equal(null);
	}
	
	public static boolean equal(List<Integer> data) {
		boolean equal = true;
		int val = data.get(0);
		for(int i=0; i<data.size(); i++) {
			if(val != data.get(i)){
				equal = false;
			}
		}
		return equal;
	}
}
