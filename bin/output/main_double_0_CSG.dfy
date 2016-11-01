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


/******************************* HARNESS ************************************/  
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

lemma LemmaCSG_covariance (a: seq<(int, int)>, b: seq<(int, int)>, key: int, yVariance: int, xVariance: int, loop0: bool)
	requires key == 1
ensures doreduce(a+b, key, yVariance, xVariance, loop0) == ((doreduce(b, key, yVariance, xVariance, loop0)+doreduce(a, key, yVariance, xVariance, loop0)))
{
	if a != []
	{
		LemmaCSG_covariance(a[1..], b, key, yVariance, xVariance, loop0);
		assert a + b == [a[0]] + (a[1..] + b);
	}
}

