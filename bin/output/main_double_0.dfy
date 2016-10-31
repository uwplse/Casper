/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class Sum{
}
class CasperDataRecord{
	var v1: int;
	var v2: int;
}


/***************************** DO MAP ***************************************/

function emit0(casper_data_set: seq<CasperDataRecord>, i0: int, i: int, loop0: bool) : seq<(int, int)>
	requires null !in casper_data_set
	reads casper_data_set
	requires 0 <= i < |casper_data_set|
	ensures emit0(casper_data_set, i0, i, loop0) == [(1,(casper_data_set[i].v1-casper_data_set[i].v2)*(casper_data_set[i].v1-casper_data_set[i].v2))]
{
	[(1,(casper_data_set[i].v1-casper_data_set[i].v2)*(casper_data_set[i].v1-casper_data_set[i].v2))]
}


function domap (casper_data_set: seq<CasperDataRecord>, i0: int, i: int, loop0: bool) : seq<(int, int)>
    requires null !in casper_data_set
	reads casper_data_set
	requires 0 <= i < |casper_data_set|
    ensures domap(casper_data_set, i0, i, loop0) == emit0(casper_data_set, i0, i, loop0)
{
    emit0(casper_data_set, i0, i, loop0)
}

/***************************** MAPPER ***************************************/

function mapper (casper_data_set: seq<CasperDataRecord>, i0: int, i: int, loop0: bool) : seq<(int, int)>
    requires null !in casper_data_set
	reads casper_data_set
	requires 0 <= i <= |casper_data_set|
{
    if i == 0 then []
    else domap(casper_data_set, i0, i-1, loop0) + mapper(casper_data_set, i0, i-1, loop0)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: seq<(int, int)>, key: int, loop0: bool) : int
    ensures (|input| > 0 && input[0].0 == key) ==> 
        doreduce(input, key, loop0) == (if key == 1 then ((input[0].1+doreduce(input[1..], key, loop0))) else ((input[0].1+doreduce(input[1..], key, loop0))))
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key, loop0) == doreduce(input[1..], key, loop0)
{
    if input == [] then (if key == 1 then 0 else 0 )
    else if input[0].0 == key then (if key == 1 then ((input[0].1+doreduce(input[1..], key, loop0))) else ((input[0].1+doreduce(input[1..], key, loop0))))
    else doreduce(input[1..], key, loop0)
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (casper_data_set: seq<CasperDataRecord>, dist: int, dist0: int, i: int, i0: int, loop0: bool)
    requires null !in casper_data_set
	reads casper_data_set
	
{
    0 <= i <= |casper_data_set| &&
	(dist == (doreduce(mapper(casper_data_set,i0,i,loop0),1, loop0)))
}

predicate postCondition (casper_data_set: seq<CasperDataRecord>, dist: int, dist0: int, i: int, i0: int, loop0: bool)
    requires null !in casper_data_set
	reads casper_data_set
	
{
    i == |casper_data_set| &&
	(dist == (doreduce(mapper(casper_data_set,i0,i,loop0),1, loop0)))
}

method harness (casper_data_set: seq<CasperDataRecord>, dist: int, i: int)
    requires null !in casper_data_set
{
    var dist0 := 0;
	var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(casper_data_set,dist0,0,i0,0,loop0);

	if(loopInvariant(casper_data_set,dist,0,i,0,loop0) && (i<|casper_data_set|))
	{
		var ind_i := i;
		ind_i := (i+1);
		var ind_dist := dist;
		ind_dist := (dist+((casper_data_set[i].v1-casper_data_set[i].v2)*(casper_data_set[i].v1-casper_data_set[i].v2)));
		assert loopInvariant(casper_data_set,ind_dist,dist0,ind_i,i0,loop0);
	}

	if(loopInvariant(casper_data_set,dist,0,i,0,loop0) && !(i<|casper_data_set|))
	{
		assert postCondition(casper_data_set,dist,0,i,0,loop0);
	}
}