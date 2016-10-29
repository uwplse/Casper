/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class Count{
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
        doreduce(input, key, loop0) == (if key == 1 then ((doreduce(input[1..], key, loop0)+1)) else ((doreduce(input[1..], key, loop0)+1)))
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key, loop0) == doreduce(input[1..], key, loop0)
{
    if input == [] then (if key == 1 then 0 else 0 )
    else if input[0].0 == key then (if key == 1 then ((doreduce(input[1..], key, loop0)+1)) else ((doreduce(input[1..], key, loop0)+1)))
    else doreduce(input[1..], key, loop0)
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (data: seq<int>, count: int, count0: int, i: int, i0: int, loop0: bool)
    
{
    0 <= i <= |data| &&
	(count == (doreduce(mapper(data,i0,i,loop0),1, loop0)))
}

predicate postCondition (data: seq<int>, count: int, count0: int, i: int, i0: int, loop0: bool)
    
{
    i == |data| &&
	(count == (doreduce(mapper(data,i0,i,loop0),1, loop0)))
}

method harness (data: seq<int>, count: int, i: int)
    
{
    var count0 := 0;
	var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(data,0,0,0,0,loop0);

	if(loopInvariant(data,count,0,i,0,loop0) && (i<|data|))
	{
		var ind_count := count;
		ind_count := (count+1);
		var ind_i := i;
		ind_i := (i+1);
		assert loopInvariant(data,ind_count,0,ind_i,0,loop0);
	}

	if(loopInvariant(data,count,0,i,0,loop0) && !(i<|data|))
	{
		assert postCondition(data,count,0,i,0,loop0);
	}
}