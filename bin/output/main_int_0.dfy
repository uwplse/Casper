/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class Count{
}


/***************************** DO MAP ***************************************/





/***************************** MAPPER ***************************************/



/***************************** DO REDUCE ************************************/

function reduce_0_count(val1: int, val2: int, loop0: bool, count0: int) : int
{
	(1+val1)
}


function doreduce_0(data: seq<int>, i: int, loop0: bool, count0: int) : int
    requires 0 <= i <= |data|
{
    if i == 0 then 0
    else (reduce_0_count(doreduce_0(data,i-1,loop0,count0),data[i-1],loop0,count0))
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (data: seq<int>, count: int, count0: int, i: int, i0: int, loop0: bool)
    
{
    0 <= i <= |data| &&
	(count == (doreduce_0(data,i,loop0,count0)))
}

predicate postCondition (data: seq<int>, count: int, count0: int, i: int, i0: int, loop0: bool)
    
{
    i == |data| &&
	(count == (doreduce_0(data,i,loop0,count0)))
}

method harness (data: seq<int>, count: int, i: int)
    
{
    var count0 := 0;
	var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(data,count0,count0,i0,i0,loop0);

	if(loopInvariant(data,count,count0,i,i0,loop0) && (i<|data|))
	{
		assert loopInvariant(data,(count+1),count0,(i+1),i0,loop0);
	}

	if(loopInvariant(data,count,count0,i,i0,loop0) && !(i<|data|))
	{
		assert postCondition(data,count,0,i,0,loop0);
	}
}