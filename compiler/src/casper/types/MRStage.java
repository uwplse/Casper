package casper.types;

import java.util.List;
import java.util.Map;

import casper.SketchParser.KvPair;

public class MRStage {
	public int stageType = -1;
	public Map<String,List<KvPair>> mapEmits = null;
	public Map<String,String> reduceExps = null;
	public Map<String,String> initExps = null;
	public Map<String,String> mergeExps = null;
}