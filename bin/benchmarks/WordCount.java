import java.util.List;
import java.util.Map;
import java.lang.Integer;
import java.lang.String;

public class WordCountJava {

	public static void main(String[] args) {
		countWords(null, null);
	}

	private static Map<String, Integer> countWords(List<String> words, Map<String, Integer> counts) {
		for (int j = 0; j < words.size(); j++) {
			String word = words.get(j);
			Integer prev = counts.get(word);
			if (prev == null)
				prev = 0;
			counts.put(word, prev + 1);
		}
		return counts;
	}
}
