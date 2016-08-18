/***************************** UTIL Functions *******************************/
function str_equal(val1: int, val2: int) : bool
{
  val1 == val2
}

/***************************** DO MAP ***************************************/
function domap (words: seq<int>, j0: int, j: int, loop0: bool) : seq<((int,int), int)>
  requires 0 <= j < |words|
  ensures domap(words, j0, j, loop0) == [((0,words[j]),1)]
{
  [((0,words[j]),1)]
}

/***************************** MAPPER ***************************************/

function mapper (words: seq<int>, j0: int, j: int, loop0: bool) : seq<((int,int), int)>
  requires 0 <= j <= |words|
{
  if j == 0 then []
  else domap(words, j0, j-1, loop0) + mapper(words, j0, j-1, loop0)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: seq<((int,int), int)>, key: (int,int)) : int
  ensures (|input| > 0 && input[0].0 == key) ==> 
      doreduce(input, key) == (input[0].1 + doreduce(input[1..], key))
  ensures (|input| > 0 && input[0].0 != key) ==> 
      doreduce(input, key) == doreduce(input[1..], key)
{
  if input == [] then 0 
  else if input[0].0 == key then (input[0].1 + doreduce(input[1..], key))
  else doreduce(input[1..], key)
}

/******************************* HARNESS ************************************/  

lemma Lemma2 (a: seq<((int,int), int)>, b: seq<((int,int), int)>, key: (int,int))
  ensures doreduce(a+b,key) == ((doreduce(b,key) + doreduce(a,key)))
{
  if a != []
  {
    Lemma2(a[1..], b, key);
    assert a + b == [a[0]] + (a[1..] + b);
  }
}

lemma Lemma (words: seq<int>, counts: seq<int>, counts0: seq<int>, j: int, j0: int, loop0: bool)
  requires |counts| == |counts0|
	requires forall k :: 0 <= k < |words| ==> 0 <= words[k] < |counts|
  requires loopInvariant(words,counts,counts0,j,0,loop0) && (j<|words|)
  
{
  assert mapper(words, j0, j+1, loop0) == domap(words, j0, j, loop0) + mapper(words, j0, j, loop0);

  assert doreduce(domap(words, j0, j, loop0),(0,words[j])) == (1);
	assert doreduce(domap(words, j0, j, loop0),(0,words[j])) == (1);
	
}

predicate loopInvariant (words: seq<int>, counts: seq<int>, counts0: seq<int>, j: int, j0: int, loop0: bool)
  requires |counts| == |counts0|
	requires forall k :: 0 <= k < |words| ==> 0 <= words[k] < |counts|
{
  0 <= j <= |words| &&
	(forall k :: 0 <= k < |counts| ==> counts[k] == doreduce(mapper(words,j0,j,loop0),(0,k)) + counts0[k])
}

predicate postCondition (words: seq<int>, counts: seq<int>, counts0: seq<int>, j: int, j0: int, loop0: bool)
  requires |counts| == |counts0|
	requires forall k :: 0 <= k < |words| ==> 0 <= words[k] < |counts|
{
  j == |words| &&
	(forall k :: 0 <= k < |counts| ==> counts[k] == doreduce(mapper(words,j0,j,loop0),(0,k)) + counts0[k])
}

method harness (words: seq<int>, counts: seq<int>, counts0: seq<int>, j: int)
  requires |counts| == |counts0|
	requires forall k :: 0 <= k < |words| ==> 0 <= words[k] < |counts|
{
  var loop0 := false;
	var j0 := 0;
	
  assert loopInvariant(words,counts0,counts0,0,0,loop0);

	if(loopInvariant(words,counts,counts0,j,0,loop0) && (j<|words|))
	{
		Lemma(words,counts,counts0,j,0,loop0);
		var ind_counts := counts;
		if((counts[words[j]]==0))
		{
			ind_counts := ind_counts[words[j] := (0+1)];
		} 
		else 
		{
			ind_counts := ind_counts[words[j] := (counts[words[j]]+1)];
		}
		var ind_j := j;
		ind_j := (j+1);
		assert loopInvariant(words,ind_counts,counts0,ind_j,0,loop0);
	}

	if(loopInvariant(words,counts,counts0,j,0,loop0) && !(j<|words|))
	{
		assert postCondition(words,counts,counts0,j,0,loop0);
	}
}