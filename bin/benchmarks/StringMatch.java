import java.lang.String;
import java.util.List;

public class StringMatch {
	
	public static void main(String[] args) {
		matchWords(null);
	}
	
	public static boolean[] matchWords(List<String> words) {
		String key1 = "key1";
		String key2 = "key2";
		
		boolean foundKey1 = false;
		boolean foundKey2 = false;
		
		for(int i=0; i<words.size(); i++) {
			if(key1.equals(words.get(i)))
				foundKey1 = true;
			if(key2.equals(words.get(i)))
				foundKey2 = true;
		}
		
		boolean[] res = {foundKey1, foundKey2};
		return res;
	}
}
