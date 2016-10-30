/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class StringMatch{
}


/***************************** DO MAP ***************************************/

function emit0(words: seq<int>, i0: int, i: int, key1: int, key2: int, loop0: bool) : seq<(int, bool)>
	requires 0 <= i < |words|
	ensures (casper_str_equal(words[i],key2)) ==> (emit0(words, i0, i, key1, key2, loop0) == [(2,true)])
{
	if casper_str_equal(words[i],key2) then [(2,true)]
	else []
}

function emit1(words: seq<int>, i0: int, i: int, key1: int, key2: int, loop0: bool) : seq<(int, bool)>
	requires 0 <= i < |words|
	ensures (casper_str_equal(words[i],key1)) ==> (emit1(words, i0, i, key1, key2, loop0) == [(1,true)])
{
	if casper_str_equal(words[i],key1) then [(1,true)]
	else []
}


function domap (words: seq<int>, i0: int, i: int, key1: int, key2: int, loop0: bool) : seq<(int, bool)>
    requires 0 <= i < |words|
    ensures domap(words, i0, i, key1, key2, loop0) == emit0(words, i0, i, key1, key2, loop0) + emit1(words, i0, i, key1, key2, loop0)
{
    emit0(words, i0, i, key1, key2, loop0) + emit1(words, i0, i, key1, key2, loop0)
}

/***************************** MAPPER ***************************************/

function mapper (words: seq<int>, i0: int, i: int, key1: int, key2: int, loop0: bool) : seq<(int, bool)>
    requires 0 <= i <= |words|
{
    if i == 0 then []
    else domap(words, i0, i-1, key1, key2, loop0) + mapper(words, i0, i-1, key1, key2, loop0)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: seq<(int, bool)>, key: int, key1: int, key2: int, loop0: bool) : bool
    ensures (|input| > 0 && input[0].0 == key) ==> 
        doreduce(input, key, key1, key2, loop0) == (if key == 1 then ((true)) else if key == 2 then ((true)) else ((true)))
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key, key1, key2, loop0) == doreduce(input[1..], key, key1, key2, loop0)
{
    if input == [] then (if key == 1 then false else if key == 2 then false else false )
    else if input[0].0 == key then (if key == 1 then ((true)) else if key == 2 then ((true)) else ((true)))
    else doreduce(input[1..], key, key1, key2, loop0)
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (words: seq<int>, foundKey2: bool, foundKey20: bool, foundKey1: bool, foundKey10: bool, i: int, i0: int, key1: int, key2: int, loop0: bool)
    
{
    0 <= i <= |words| &&
	(foundKey1 == (doreduce(mapper(words,i0,i,key1,key2,loop0),1, key1, key2, loop0)||foundKey10)) &&
	(foundKey2 == (doreduce(mapper(words,i0,i,key1,key2,loop0),2, key1, key2, loop0)||foundKey20))
}

predicate postCondition (words: seq<int>, foundKey2: bool, foundKey20: bool, foundKey1: bool, foundKey10: bool, i: int, i0: int, key1: int, key2: int, loop0: bool)
    
{
    i == |words| &&
	(foundKey1 == (doreduce(mapper(words,i0,i,key1,key2,loop0),1, key1, key2, loop0)||foundKey10)) &&
	(foundKey2 == (doreduce(mapper(words,i0,i,key1,key2,loop0),2, key1, key2, loop0)||foundKey20))
}

method harness (words: seq<int>, foundKey1: bool, foundKey2: bool, key1: int, key2: int, i: int)
    
{
    var foundKey10 := false;
	var foundKey20 := false;
	var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(words,false,false,false,false,0,0,key1,key2,loop0);

	if(loopInvariant(words,foundKey2,false,foundKey1,false,i,0,key1,key2,loop0) && (i<|words|))
	{
		var ind_foundKey2 := foundKey2;
		if(casper_str_equal(key2,words[i]))
		{
			ind_foundKey2 := true;
		} else 
		{
			ind_foundKey2 := foundKey2;
		}
		var ind_foundKey1 := foundKey1;
		if(casper_str_equal(key1,words[i]))
		{
			ind_foundKey1 := true;
		} else 
		{
			ind_foundKey1 := foundKey1;
		}
		var ind_i := i;
		ind_i := (i+1);
		assert loopInvariant(words,ind_foundKey2,false,ind_foundKey1,false,ind_i,0,key1,key2,loop0);
	}

	if(loopInvariant(words,foundKey2,false,foundKey1,false,i,0,key1,key2,loop0) && !(i<|words|))
	{
		assert postCondition(words,foundKey2,false,foundKey1,false,i,0,key1,key2,loop0);
	}
}