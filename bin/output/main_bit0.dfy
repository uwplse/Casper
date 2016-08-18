/***************************** UTIL Functions *******************************/
function str_equal(val1: int, val2: int) : bool
{
  val1 == val2
}

/***************************** DO MAP ***************************************/
function domap (data: seq<int>, i0: int, i: int, loop0: bool, val: int) : seq<(int, bool)>
  requires 0 <= i < |data|
  ensures domap(data, i0, i, loop0, val) == [(0,data[i] == val)]
{
  [(0,data[i] == val)]
}

/***************************** MAPPER ***************************************/

function mapper (data: seq<int>, i0: int, i: int, loop0: bool, val: int) : seq<(int, bool)>
  requires 0 <= i <= |data|
{
  if i == 0 then []
  else domap(data, i0, i-1, loop0, val) + mapper(data, i0, i-1, loop0, val)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: seq<(int, bool)>, key: int) : bool
  ensures (|input| > 0 && input[0].0 == key) ==> 
      doreduce(input, key) == (input[0].1 && doreduce(input[1..], key))
  ensures (|input| > 0 && input[0].0 != key) ==> 
      doreduce(input, key) == doreduce(input[1..], key)
{
  if input == [] then true 
  else if input[0].0 == key then (input[0].1 && doreduce(input[1..], key))
  else doreduce(input[1..], key)
}

/******************************* HARNESS ************************************/  

lemma Lemma2 (a: seq<(int, bool)>, b: seq<(int, bool)>, key: int)
  ensures doreduce(a+b,key) == ((doreduce(b,key) && doreduce(a,key)))
{
  if a != []
  {
    Lemma2(a[1..], b, key);
    assert a + b == [a[0]] + (a[1..] + b);
  }
}

lemma Lemma (data: seq<int>, equal: bool, equal0: bool, i: int, i0: int, loop0: bool, val: int)
  
  requires loopInvariant(data,equal,true,i,0,loop0,val) && (i<|data|)
  
{
  assert mapper(data, i0, i+1, loop0, val) == domap(data, i0, i, loop0, val) + mapper(data, i0, i, loop0, val);

  assert doreduce(domap(data, i0, i, loop0, val),0) == (data[i] == val);
	Lemma2(domap(data, i0, i, loop0, val),mapper(data, i0, i, loop0, val),0);
	assert doreduce(mapper(data, i0, i+1, loop0, val),0) == (doreduce(domap(data, i0, i, loop0, val),0) && doreduce(mapper(data, i0, i, loop0, val),0));

	
}

predicate loopInvariant (data: seq<int>, equal: bool, equal0: bool, i: int, i0: int, loop0: bool, val: int)
  
{
  0 <= i <= |data| &&
	equal == doreduce(mapper(data,i0,i,loop0,val),0)
}

predicate postCondition (data: seq<int>, equal: bool, equal0: bool, i: int, i0: int, loop0: bool, val: int)
  
{
  i == |data| &&
	equal == doreduce(mapper(data,i0,i,loop0,val),0)
}

method harness (data: seq<int>, equal: bool, val: int, i: int)
  
{
  var equal0 := true;
	var loop0 := false;
	var i0 := 0;
	
  assert loopInvariant(data,true,true,0,0,loop0,val);

	if(loopInvariant(data,equal,true,i,0,loop0,val) && (i<|data|))
	{
		Lemma(data,equal,true,i,0,loop0,val);
		var ind_equal := equal;
		if((val!=data[i]))
		{
			ind_equal := false;
		} else 
		{
			ind_equal := equal;
		}
		var ind_i := i;
		ind_i := (i+1);
		assert loopInvariant(data,ind_equal,true,ind_i,0,loop0,val);
	}

	if(loopInvariant(data,equal,true,i,0,loop0,val) && !(i<|data|))
	{
		assert postCondition(data,equal,true,i,0,loop0,val);
	}
}