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


/******************************* HARNESS ************************************/  
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

lemma LemmaCSG_result (casper_a: int, casper_b: int, casper_c: int, result0: seq<int>)
  ensures reduce_result(casper_a,casper_b, result0) == reduce_result(casper_b,casper_a, result0)
  ensures reduce_result(reduce_result(casper_a,casper_b, result0),casper_c, result0) == reduce_result(casper_a,reduce_result(casper_b,casper_c, result0), result0)
{}