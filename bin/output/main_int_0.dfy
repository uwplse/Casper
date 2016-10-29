/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class Sum{
}


/***************************** DO MAP ***************************************/

function emit0(data: seq<int>, i0: int, i: int, loop0: bool) : seq<(int, (int,int))>
	requires 0 <= i < |data|
	ensures emit0(data, i0, i, loop0) == [(1,(data[i]+data[i],data[i]))]
{
	[(1,(data[i]+data[i],data[i]))]
}


function domap (data: seq<int>, i0: int, i: int, loop0: bool) : seq<(int, (int,int))>
    requires 0 <= i < |data|
    ensures domap(data, i0, i, loop0) == emit0(data, i0, i, loop0)
{
    emit0(data, i0, i, loop0)
}

/***************************** MAPPER ***************************************/

function mapper (data: seq<int>, i0: int, i: int, loop0: bool) : seq<(int, (int,int))>
    requires 0 <= i <= |data|
{
    if i == 0 then []
    else domap(data, i0, i-1, loop0) + mapper(data, i0, i-1, loop0)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: seq<(int, (int,int))>, key: int, loop0: bool) : int
    ensures (|input| > 0 && input[0].0 == key) ==> 
        doreduce(input, key, loop0) == (if key == 1 then ((input[0].1.1+doreduce(input[1..], key, loop0))) else ((input[0].1.1+doreduce(input[1..], key, loop0))))
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key, loop0) == doreduce(input[1..], key, loop0)
{
    if input == [] then (if key == 1 then 0 else 0 )
    else if input[0].0 == key then (if key == 1 then ((input[0].1.1+doreduce(input[1..], key, loop0))) else ((input[0].1.1+doreduce(input[1..], key, loop0))))
    else doreduce(input[1..], key, loop0)
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (data: seq<int>, sum: int, sum0: int, i: int, i0: int, loop0: bool)
    
{
    0 <= i <= |data| &&
	(sum == (doreduce(mapper(data,i0,i,loop0),1, loop0)))
}

predicate postCondition (data: seq<int>, sum: int, sum0: int, i: int, i0: int, loop0: bool)
    
{
    i == |data| &&
	(sum == (doreduce(mapper(data,i0,i,loop0),1, loop0)))
}

method harness (data: seq<int>, sum: int, i: int)
    
{
    var sum0 := 0;
	var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(data,0,0,0,0,loop0);

	if(loopInvariant(data,sum,0,i,0,loop0) && (i<|data|))
	{
		var ind_sum := sum;
		ind_sum := (sum+data[i]);
		var ind_i := i;
		ind_i := (i+1);
		assert loopInvariant(data,ind_sum,0,ind_i,0,loop0);
	}

	if(loopInvariant(data,sum,0,i,0,loop0) && !(i<|data|))
	{
		assert postCondition(data,sum,0,i,0,loop0);
	}
}