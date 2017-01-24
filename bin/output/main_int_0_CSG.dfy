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


/******************************* HARNESS ************************************/  
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
	requires 0 <= casper_key.1 < |hR0|
	requires 0 <= casper_key.1 < |hG0|
	requires 0 <= casper_key.1 < |hB0|
	
    ensures (|input| > 0 && input[0].0 == casper_key) ==> 
        doreduce(input, casper_key, loop0, hR0, hG0, hB0) == (if casper_key.0 == 1 then reduce_hR(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 2 then reduce_hG(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 3 then reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else (reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0)))
    ensures (|input| > 0 && input[0].0 != casper_key) ==> 
        doreduce(input, casper_key, loop0, hR0, hG0, hB0) == doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0)
{
    if input == [] then (if casper_key.0 == 1 then hR0[casper_key.1] else if casper_key.0 == 2 then hG0[casper_key.1] else if casper_key.0 == 3 then hB0[casper_key.1] else hB0[casper_key.1] )
    else if input[0].0 == casper_key then (if casper_key.0 == 1 then reduce_hR(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 2 then reduce_hG(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else if casper_key.0 == 3 then reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0) else (reduce_hB(doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0),input[0].1, loop0, hR0, hG0, hB0)))
    else doreduce(input[1..], casper_key, loop0, hR0, hG0, hB0)
}  

lemma LemmaCSG_hR (casper_a: int, casper_b: int, casper_c: int, loop0: bool, hR0: seq<int>, hG0: seq<int>, hB0: seq<int>)
  ensures reduce_hR(casper_a,casper_b, loop0, hR0, hG0, hB0) == reduce_hR(casper_b,casper_a, loop0, hR0, hG0, hB0)
  ensures reduce_hR(reduce_hR(casper_a,casper_b, loop0, hR0, hG0, hB0),casper_c, loop0, hR0, hG0, hB0) == reduce_hR(casper_a,reduce_hR(casper_b,casper_c, loop0, hR0, hG0, hB0), loop0, hR0, hG0, hB0)
{}lemma LemmaCSG_hG (casper_a: int, casper_b: int, casper_c: int, loop0: bool, hR0: seq<int>, hG0: seq<int>, hB0: seq<int>)
  ensures reduce_hG(casper_a,casper_b, loop0, hR0, hG0, hB0) == reduce_hG(casper_b,casper_a, loop0, hR0, hG0, hB0)
  ensures reduce_hG(reduce_hG(casper_a,casper_b, loop0, hR0, hG0, hB0),casper_c, loop0, hR0, hG0, hB0) == reduce_hG(casper_a,reduce_hG(casper_b,casper_c, loop0, hR0, hG0, hB0), loop0, hR0, hG0, hB0)
{}lemma LemmaCSG_hB (casper_a: int, casper_b: int, casper_c: int, loop0: bool, hR0: seq<int>, hG0: seq<int>, hB0: seq<int>)
  ensures reduce_hB(casper_a,casper_b, loop0, hR0, hG0, hB0) == reduce_hB(casper_b,casper_a, loop0, hR0, hG0, hB0)
  ensures reduce_hB(reduce_hB(casper_a,casper_b, loop0, hR0, hG0, hB0),casper_c, loop0, hR0, hG0, hB0) == reduce_hB(casper_a,reduce_hB(casper_b,casper_c, loop0, hR0, hG0, hB0), loop0, hR0, hG0, hB0)
{}