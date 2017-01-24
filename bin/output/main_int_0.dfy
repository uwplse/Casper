/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class HistogramJava{
}
class Pixel{
	var b: int;
	var g: int;
	var r: int;
}


/***************************** DO MAP ***************************************/

function emit0(image: seq<Pixel>, i0: int, i: int, loop0: bool) : seq<((int,int), int)>
	requires null !in image
	reads image
	requires 0 <= i < |image|
	ensures emit0(image, i0, i, loop0) == [((2,image[i].g),1)]
{
	[((2,image[i].g),1)]
}

function emit1(image: seq<Pixel>, i0: int, i: int, loop0: bool) : seq<((int,int), int)>
	requires null !in image
	reads image
	requires 0 <= i < |image|
	ensures emit1(image, i0, i, loop0) == [((3,image[i].b),1)]
{
	[((3,image[i].b),1)]
}

function emit2(image: seq<Pixel>, i0: int, i: int, loop0: bool) : seq<((int,int), int)>
	requires null !in image
	reads image
	requires 0 <= i < |image|
	ensures emit2(image, i0, i, loop0) == [((1,image[i].r),1)]
{
	[((1,image[i].r),1)]
}


function domap (image: seq<Pixel>, i0: int, i: int, loop0: bool) : seq<((int,int), int)>
    requires null !in image
	reads image
	requires 0 <= i < |image|
    ensures domap(image, i0, i, loop0) == emit0(image, i0, i, loop0) + emit1(image, i0, i, loop0) + emit2(image, i0, i, loop0)
{
    emit0(image, i0, i, loop0) + emit1(image, i0, i, loop0) + emit2(image, i0, i, loop0)
}

/***************************** MAPPER ***************************************/

function mapper (image: seq<Pixel>, i0: int, i: int, loop0: bool) : seq<((int,int), int)>
    requires null !in image
	reads image
	requires 0 <= i <= |image|
{
    if i == 0 then []
    else domap(image, i0, i-1, loop0) + mapper(image, i0, i-1, loop0)
}

/***************************** DO REDUCE ************************************/
function reduce_hR(val1: int, val2: int, loop0: bool, hR0: seq<int>, hG0: seq<int>, hB0: seq<int>) : int
{
	(val2+val1)
}
function reduce_hG(val1: int, val2: int, loop0: bool, hR0: seq<int>, hG0: seq<int>, hB0: seq<int>) : int
{
	(val2+val1)
}
function reduce_hB(val1: int, val2: int, loop0: bool, hR0: seq<int>, hG0: seq<int>, hB0: seq<int>) : int
{
	(val1+val2)
}


function doreduce(input: seq<((int,int), int)>, casper_key: (int,int), loop0: bool, hR0: seq<int>, hG0: seq<int>, hB0: seq<int>) : int
    requires casper_key.0 == 1 ==> 0 <= casper_key.1 < |hR0|
	requires casper_key.0 == 2 ==> 0 <= casper_key.1 < |hG0|
	requires casper_key.0 == 3 ==> 0 <= casper_key.1 < |hB0|
	
    ensures (|input| > 0 && input[0].0 == casper_key) ==> 
        doreduce(input, casper_key, loop0, hR0, hG0, hB0) == (if casper_key.0 == 1 then reduce_hR(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 2 then reduce_hG(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 3 then reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else (reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0)))
    ensures (|input| > 0 && input[0].0 != casper_key) ==> 
        doreduce(input, casper_key, loop0, hR0, hG0, hB0) == doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0)
{
    if input == [] then (if casper_key.0 == 1 then hR0[casper_key.1] else if casper_key.0 == 2 then hG0[casper_key.1] else if casper_key.0 == 3 then hB0[casper_key.1] else 0 )
    else if input[0].0 == casper_key then (if casper_key.0 == 1 then reduce_hR(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 2 then reduce_hG(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 3 then reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else (reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0)))
    else doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0)
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (image: seq<Pixel>, hB: seq<int>, hB0: seq<int>, hG: seq<int>, hG0: seq<int>, hR: seq<int>, hR0: seq<int>, i: int, i0: int, loop0: bool)
    requires null !in image
	reads image
	requires  |hR| == |hR0|
	requires  |hG| == |hG0|
	requires  |hB| == |hB0|
	//requires  |hR| == |hG| && |hG| == |hB|
{
    0 <= i <= |image| &&
	(forall k :: 0 <= k < |hR| ==> hR[k] == (doreduce(mapper(image,i0,i,loop0),(1,k), loop0, hR0, hG0, hB0)))	 &&
	(forall k :: 0 <= k < |hG| ==> hG[k] == (doreduce(mapper(image,i0,i,loop0),(2,k), loop0, hR0, hG0, hB0)))	 &&
	(forall k :: 0 <= k < |hB| ==> hB[k] == (doreduce(mapper(image,i0,i,loop0),(3,k), loop0, hR0, hG0, hB0)))	
}

predicate postCondition (image: seq<Pixel>, hB: seq<int>, hB0: seq<int>, hG: seq<int>, hG0: seq<int>, hR: seq<int>, hR0: seq<int>, i: int, i0: int, loop0: bool)
    requires null !in image
	reads image
	requires |hR| == |hR0|
	requires |hG| == |hG0|
	requires |hB| == |hB0|
    //requires  |hR| == |hG| && |hG| == |hB|	
{
    i == |image| &&
	(forall k :: 0 <= k < |hR| ==> hR[k] == (doreduce(mapper(image,i0,i,loop0),(1,k), loop0, hR0, hG0, hB0)))	 &&
	(forall k :: 0 <= k < |hG| ==> hG[k] == (doreduce(mapper(image,i0,i,loop0),(2,k), loop0, hR0, hG0, hB0)))	 &&
	(forall k :: 0 <= k < |hB| ==> hB[k] == (doreduce(mapper(image,i0,i,loop0),(3,k), loop0, hR0, hG0, hB0)))	
}

method harness (image: seq<Pixel>, hR: seq<int>, hR0: seq<int>, hG: seq<int>, hG0: seq<int>, hB: seq<int>, hB0: seq<int>, i: int)
    requires null !in image
	requires  |hR| == |hR0|
	requires  |hG| == |hG0|
	requires  |hB| == |hB0|
    //requires  |hR| == |hG| && |hG| == |hB| 
	requires 0 <= i < |image| ==> 0 <= image[i].r < |hR|
	requires 0 <= i < |image| ==> 0 <= image[i].g < |hG|
	requires 0 <= i < |image| ==> 0 <= image[i].b < |hB|
{
    var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(image,hB0,hB0,hG0,hG0,hR0,hR0,i0,i0,loop0);

	if(loopInvariant(image,hB,hB0,hG,hG0,hR,hR0,i,i0,loop0) && (i<|image|))
	{
		var ind_hG := hG;
		ind_hG := ind_hG[image[i].g := (hG[image[i].g]+1)];
		var ind_hR := hR;
		ind_hR := ind_hR[image[i].r := (hR[image[i].r]+1)];
		var ind_hB := hB;
		ind_hB := ind_hB[image[i].b := (hB[image[i].b]+1)];
		var ind_i := i;
		ind_i := (i+1);
		assert loopInvariant(image,ind_hB,hB0,ind_hG,hG0,ind_hR,hR0,ind_i,i0,loop0);
	}

	if(loopInvariant(image,hB,hB0,hG,hG0,hR,hR0,i,i0,loop0) && !(i<|image|))
	{
		assert postCondition(image,hB,hB0,hG,hG0,hR,hR0,i,0,loop0);
	}
}
