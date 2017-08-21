/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class MulVecSca{
}


/***************************** DO MAP ***************************************/

function emit_0_0(a: seq<int>, i0: int, i: int, loop0: bool, b: int) : <domap-emit-type>
	<emit-requires>
	ensures emit_0_0(a, i0, i, loop0, b) == [((1,i),(b*a[i],b))]
{
	[((1,i),(b*a[i],b))]
}



function domap_0 (a: seq<int>, i0: int, i: int, loop0: bool, b: int) : <domap-emit-type>
    <emit-requires>
    ensures domap_0(a, i0, i, loop0, b) == emit_0_0(a, i0, i, loop0, b)
{
    emit_0_0(a, i0, i, loop0, b)
}

/***************************** MAPPER ***************************************/

function mapper_0 (a: seq<int>, i0: int, i: int, loop0: bool, b: int) : <domap-emit-type>
    <mapper-requires>
{
    if <terminate-condition> then []
    else domap_0(a, i0, i-1, loop0, b) + mapper_0(a, i0, i-1, loop0, b)
}

/***************************** DO REDUCE ************************************/





/******************************* HARNESS ************************************/    

predicate loopInvariant (a: seq<int>, temp: seq<int>, temp0: seq<int>, i: int, i0: int, loop0: bool, b: int)
    requires  |temp| == |temp0|
	
{
    0 <= i <= |a| &&
	(forall k :: 0 <= k < |temp| ==> temp[k] == (mapper_0(,loop0,b,temp0))[k])	
}

predicate postCondition (a: seq<int>, temp: seq<int>, temp0: seq<int>, i: int, i0: int, loop0: bool, b: int)
    requires  |temp| == |temp0|
	
{
    i == |a| &&
	(forall k :: 0 <= k < |temp| ==> temp[k] == (mapper_0(,loop0,b,temp0))[k])	
}

method harness (a: seq<int>, temp: seq<int>, temp0: seq<int>, b: int, i: int)
    requires  |temp| == |temp0|
	requires  forall k :: 0 <= k < |temp0| ==> temp0[k] == 0
{
    var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(a,temp0,temp0,i0,i0,loop0,b);

	if(loopInvariant(a,temp,temp0,i,i0,loop0,b) && (i<|a|))
	{
		assert loopInvariant(a,int_setter(temp,i,(int_getter(a,i)*b)),temp0,(i+1),i0,loop0,b);
	}

	if(loopInvariant(a,temp,temp0,i,i0,loop0,b) && !(i<|a|))
	{
		assert postCondition(a,temp,temp0,i,0,loop0,b);
	}
}