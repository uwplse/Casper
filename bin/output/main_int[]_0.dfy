/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class YelpKids2{
}
class Restaurant{
	var goodForKids: bool;
	var score: int;
	var comment: int;
	var city: int;
	var state: int;
}


/***************************** DO MAP ***************************************/

function emit0(data: seq<Restaurant>, casper_index0: int, casper_index: int) : seq<((int,int), int)>
	requires null !in data
	reads data
	requires 0 <= casper_index < |data|
	ensures (data[casper_index].goodForKids) ==> (emit0(data, casper_index0, casper_index) == [((1,data[casper_index].city),1)])
{
	if data[casper_index].goodForKids then [((1,data[casper_index].city),1)]
	else []
}


function domap (data: seq<Restaurant>, casper_index0: int, casper_index: int) : seq<((int,int), int)>
    requires null !in data
	reads data
	requires 0 <= casper_index < |data|
    ensures domap(data, casper_index0, casper_index) == emit0(data, casper_index0, casper_index)
{
    emit0(data, casper_index0, casper_index)
}

/***************************** MAPPER ***************************************/

function mapper (data: seq<Restaurant>, casper_index0: int, casper_index: int) : seq<((int,int), int)>
    requires null !in data
	reads data
	requires 0 <= casper_index <= |data|
{
    if casper_index == 0 then []
    else domap(data, casper_index0, casper_index-1) + mapper(data, casper_index0, casper_index-1)
}

/***************************** DO REDUCE ************************************/
function reduce_result(val1: int, val2: int, result0: seq<int>) : int
{
	(val2+val1)
}


function doreduce(input: seq<((int,int), int)>, casper_key: (int,int), result0: seq<int>) : int
    requires 0 <= casper_key.1 < |result0|
	
    ensures (|input| > 0 && input[0].0 == casper_key) ==> 
        doreduce(input, casper_key, result0) == (if casper_key.0 == 1 then reduce_result(doreduce(input[1..], casper_key, result0),input[0].1, result0) else (reduce_result(doreduce(input[1..], casper_key, result0),input[0].1, result0)))
    ensures (|input| > 0 && input[0].0 != casper_key) ==> 
        doreduce(input, casper_key, result0) == doreduce(input[1..], casper_key, result0)
{
    if input == [] then (if casper_key.0 == 1 then result0[casper_key.1] else result0[casper_key.1] )
    else if input[0].0 == casper_key then (if casper_key.0 == 1 then reduce_result(doreduce(input[1..], casper_key, result0),input[0].1, result0) else (reduce_result(doreduce(input[1..], casper_key, result0),input[0].1, result0)))
    else doreduce(input[1..], casper_key, result0)
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (data: seq<Restaurant>, result: seq<int>, result0: seq<int>, casper_index: int, casper_index0: int)
    requires null !in data
	reads data
	requires  |result| == |result0|
	
{
    0 <= casper_index <= |data| &&
	(forall k :: 0 <= k < |result| ==> result[k] == (doreduce(mapper(data,casper_index0,casper_index),(1,k), result0)))	
}

predicate postCondition (data: seq<Restaurant>, result: seq<int>, result0: seq<int>, casper_index: int, casper_index0: int)
    requires null !in data
	reads data
	requires  |result| == |result0|
	
{
    casper_index == |data| &&
	(forall k :: 0 <= k < |result| ==> result[k] == (doreduce(mapper(data,casper_index0,casper_index),(1,k), result0)))	
}

method harness (data: seq<Restaurant>, result: seq<int>, result0: seq<int>, casper_index: int)
    requires null !in data
	requires  |result| == |result0|
	requires  forall k :: 0 <= k < |result0| ==> result0[k] == 0
	requires 0 <= casper_index < |data| ==> 0 <= data[casper_index].city < |result|
{
    var casper_index0 := 0;
	
    assert loopInvariant(data,result0,result0,casper_index0,casper_index0);

	if(loopInvariant(data,result,result0,casper_index,casper_index0) && (casper_index<|data|))
	{
		var ind_result := result;
		if((!(result[data[casper_index].city]!=0)))
		{
			ind_result := ind_result[data[casper_index].city := 0];
		if(data[casper_index].goodForKids)
		{
			ind_result := ind_result[data[casper_index].city := (result[data[casper_index].city]+1)];
		} 
		else 
		{
			ind_result := result;
		}
		
		} else {
			if(data[casper_index].goodForKids)
		{
			ind_result := ind_result[data[casper_index].city := (result[data[casper_index].city]+1)];
		} 
		else 
		{
			ind_result := result;
		}
		}
		var ind_casper_index := casper_index;
		ind_casper_index := (casper_index+1);
		assert loopInvariant(data,ind_result,result0,ind_casper_index,casper_index0);
	}

	if(loopInvariant(data,result,result0,casper_index,casper_index0) && !(casper_index<|data|))
	{
		assert postCondition(data,result,result0,casper_index,0);
	}
}
