/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
class StatsUtilList{
	var period: int;
}
class CasperDataRecord{
	var x: int;
	var y: int;
}


/***************************** DO MAP ***************************************/

function emit0(casper_data_set: seq<CasperDataRecord>, i0: int, i: int, yVariance: int, xVariance: int, loop0: bool) : seq<(int, int)>
	requires null !in casper_data_set
	reads casper_data_set
	requires 0 <= i < |casper_data_set|
	ensures emit0(casper_data_set, i0, i, yVariance, xVariance, loop0) == [(1,(casper_data_set[i].x-xVariance)*(casper_data_set[i].y-yVariance))]
{
	[(1,(casper_data_set[i].x-xVariance)*(casper_data_set[i].y-yVariance))]
}


function domap (casper_data_set: seq<CasperDataRecord>, i0: int, i: int, yVariance: int, xVariance: int, loop0: bool) : seq<(int, int)>
    requires null !in casper_data_set
	reads casper_data_set
	requires 0 <= i < |casper_data_set|
    ensures domap(casper_data_set, i0, i, yVariance, xVariance, loop0) == emit0(casper_data_set, i0, i, yVariance, xVariance, loop0)
{
    emit0(casper_data_set, i0, i, yVariance, xVariance, loop0)
}

/***************************** MAPPER ***************************************/

function mapper (casper_data_set: seq<CasperDataRecord>, i0: int, i: int, yVariance: int, xVariance: int, loop0: bool) : seq<(int, int)>
    requires null !in casper_data_set
	reads casper_data_set
	requires 0 <= i <= |casper_data_set|
{
    if i == 0 then []
    else domap(casper_data_set, i0, i-1, yVariance, xVariance, loop0) + mapper(casper_data_set, i0, i-1, yVariance, xVariance, loop0)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: seq<(int, int)>, key: int, yVariance: int, xVariance: int, loop0: bool) : int
    ensures (|input| > 0 && input[0].0 == key) ==> 
        doreduce(input, key, yVariance, xVariance, loop0) == (if key == 1 then ((input[0].1+doreduce(input[1..], key, yVariance, xVariance, loop0))) else ((input[0].1+doreduce(input[1..], key, yVariance, xVariance, loop0))))
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key, yVariance, xVariance, loop0) == doreduce(input[1..], key, yVariance, xVariance, loop0)
{
    if input == [] then (if key == 1 then 0 else 0 )
    else if input[0].0 == key then (if key == 1 then ((input[0].1+doreduce(input[1..], key, yVariance, xVariance, loop0))) else ((input[0].1+doreduce(input[1..], key, yVariance, xVariance, loop0))))
    else doreduce(input[1..], key, yVariance, xVariance, loop0)
}

/******************************* HARNESS ************************************/    

predicate loopInvariant (casper_data_set: seq<CasperDataRecord>, covariance: int, covariance0: int, i: int, i0: int, yVariance: int, xVariance: int, loop0: bool)
    requires null !in casper_data_set
	reads casper_data_set
	
{
    0 <= i <= |casper_data_set| &&
	(covariance == (doreduce(mapper(casper_data_set,i0,i,yVariance,xVariance,loop0),1, yVariance, xVariance, loop0)))
}

predicate postCondition (casper_data_set: seq<CasperDataRecord>, covariance: int, covariance0: int, i: int, i0: int, yVariance: int, xVariance: int, loop0: bool)
    requires null !in casper_data_set
	reads casper_data_set
	
{
    i == |casper_data_set| &&
	(covariance == (doreduce(mapper(casper_data_set,i0,i,yVariance,xVariance,loop0),1, yVariance, xVariance, loop0)))
}

method harness (casper_data_set: seq<CasperDataRecord>, covariance: int, yVariance: int, xVariance: int, i: int)
    requires null !in casper_data_set
{
    var covariance0 := 0;
	var loop0 := false;
	var i0 := 0;
	
    assert loopInvariant(casper_data_set,covariance0,0,i0,0,yVariance,xVariance,loop0);

	if(loopInvariant(casper_data_set,covariance,0,i,0,yVariance,xVariance,loop0) && (i<|casper_data_set|))
	{
		var ind_covariance := covariance;
		ind_covariance := (covariance+((casper_data_set[i].x-xVariance)*(casper_data_set[i].y-yVariance)));
		var ind_i := i;
		ind_i := (i+1);
		assert loopInvariant(casper_data_set,ind_covariance,covariance0,ind_i,i0,yVariance,xVariance,loop0);
	}

	if(loopInvariant(casper_data_set,covariance,0,i,0,yVariance,xVariance,loop0) && !(i<|casper_data_set|))
	{
		assert postCondition(casper_data_set,covariance,0,i,0,yVariance,xVariance,loop0);
	}
}