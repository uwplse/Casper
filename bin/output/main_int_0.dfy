/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class Average{
}


/***************************** DO MAP ***************************************/

function emit0(data: seq<int>, i0: int, i: int, loop0: bool) : seq<(int, int)>
	requires 0 <= i < |data|
	ensures emit0(data, i0, i, loop0) == [(1,data[i])]
{
	[(1,data[i])]
}

function emit1(data: seq<int>, i0: int, i: int, loop0: bool) : seq<(int, int)>
	requires 0 <= i < |data|
	ensures emit1(data, i0, i, loop0) == [(2,16)]
{
	[(2,16)]
}


function domap (data: seq<int>, i0: int, i: int, loop0: bool) : seq<(int, int)>
    requires 0 <= i < |data|
    ensures domap(data, i0, i, loop0) == emit0(data, i0, i, loop0) + emit1(data, i0, i, loop0)
{
    emit0(data, i0, i, loop0) + emit1(data, i0, i, loop0)
}

/***************************** MAPPER ***************************************/

function mapper (data: seq<int>, i0: int, i: int, loop0: bool) : seq<(int, int)>
    requires 0 <= i <= |data|
{
    if i == 0 then []
    else domap(data, i0, i-1, loop0) + mapper(data, i0, i-1, loop0)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: seq<(int, int)>, key: int, loop0: bool) : int
    ensures (|input| > 0 && input[0].0 == key) ==> 
        doreduce(input, key, loop0) == (if key == 1 then ((input[0].1+doreduce(input[1..], key, loop0))) else if key == 2 then ((doreduce(input[1..], key, loop0)+1)) else ((doreduce(input[1..], key, loop0)+1)))
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key, loop0) == doreduce(input[1..], key, loop0)
{
    if input == [] then (if key == 1 then 0 else if key == 2 then 0 else 0 )
    else if input[0].0 == key then (if key == 1 then ((input[0].1+doreduce(input[1..], key, loop0))) else if key == 2 then ((doreduce(input[1..], key, loop0)+1)) else ((doreduce(input[1..], key, loop0)+1)))
    else doreduce(input[1..], key, loop0)
}

/******************************* HARNESS ************************************/    

lemma LemmaCSG_sum (a: seq<(int, int)>, b: seq<(int, int)>, key: int, loop0: bool)
	requires key == 1
ensures doreduce(a+b, key, loop0) == ((doreduce(b, key, loop0)+doreduce(a, key, loop0)))
{
	if a != []
	{
		LemmaCSG_sum(a[1..], b, key, loop0);
		assert a + b == [a[0]] + (a[1..] + b);
	}
}

lemma LemmaCSG_count (a: seq<(int, int)>, b: seq<(int, int)>, key: int, loop0: bool)
	requires key == 2
ensures doreduce(a+b, key, loop0) == ((doreduce(a, key, loop0)+1))
{
	if a != []
	{
		LemmaCSG_count(a[1..], b, key, loop0);
		assert a + b == [a[0]] + (a[1..] + b);
	}
}




predicate loopInvariant (data: seq<int>, count: int, count0: int, sum: int, sum0: int, i: int, i0: int, loop0: bool)
    
{
    0 <= i <= |data| &&
	(sum == (doreduce(mapper(data,i0,i,loop0),1, loop0))) &&
	(count == (doreduce(mapper(data,i0,i,loop0),2, loop0)))
}

predicate postCondition (data: seq<int>, count: int, count0: int, sum: int, sum0: int, i: int, i0: int, loop0: bool)
    
{
    i == |data| &&
	(sum == (doreduce(mapper(data,i0,i,loop0),1, loop0))) &&
	(count == (doreduce(mapper(data,i0,i,loop0),2, loop0)))
}

method harness (data: seq<int>, sum: int, count: int, i: int)
    
{
    var sum0 := 0;
	var count0 := 0;
	var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(data,0,0,0,0,0,0,loop0);

	if(loopInvariant(data,count,0,sum,0,i,0,loop0) && (i<|data|))
	{
		var ind_count := count;
		ind_count := (count+1);
		var ind_sum := sum;
		ind_sum := (sum+data[i]);
		var ind_i := i;
		ind_i := (i+1);
		assert loopInvariant(data,ind_count,0,ind_sum,0,ind_i,0,loop0);
	}

	if(loopInvariant(data,count,0,sum,0,i,0,loop0) && !(i<|data|))
	{
		assert postCondition(data,count,0,sum,0,i,0,loop0);
	}
}