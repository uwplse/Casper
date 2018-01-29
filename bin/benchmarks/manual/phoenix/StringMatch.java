package manual.phoenix;

import org.apache.spark.api.java.JavaRDD;

public class StringMatch {
	public static boolean[] matchWords(JavaRDD<String> words) {
		return words.aggregate(new boolean[3],
										(res, word) -> {
												res[0] = res[0] || (word == "key1");
												res[1] = res[1] || (word == "key2");
												res[2] = res[2] || (word == "key3");
												return res;
										},
										(res1, res2) -> {
												res1[0] = res1[0] || res2[0];
												res1[1] = res1[1] || res2[1];
												res1[2] = res1[2] || res2[2];
												return res1;
										}
									 );
	}
}