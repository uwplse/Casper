/***************************** INCLUDES *************************************/
include "utils.dfy"

function doreduce(input: <domap-emit-type>, key: <doreduce-key-type><reducer-args-decl>) : <output-type>
    ensures (|input| > 0 && input[0].0 == key) ==> 
        doreduce(input, key<reducer-args-call>) == (<reduce-exp>)
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key<reducer-args-call>) == doreduce(input[1..], key<reducer-args-call>)
{
    if input == [] then (<reduce-init-value> )
    else if input[0].0 == key then (<reduce-exp>)
    else doreduce(input[1..], key<reducer-args-call>)
}

/******************************* HARNESS ************************************/    

<reduce-exp-lemma>