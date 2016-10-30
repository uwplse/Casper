/***************************** INCLUDES *************************************/
include "utils.dfy"

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

