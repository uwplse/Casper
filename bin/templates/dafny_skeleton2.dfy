/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
<udts>

/******************************* HARNESS ************************************/  
<reduce-functions>

function doreduce(input: <domap-emit-type>, casper_key: <doreduce-key-type><reducer-args-decl>) : <output-type>
	<key-requires>
    ensures (|input| > 0 && input[0].0 == casper_key) ==> 
        doreduce(input, casper_key<reducer-args-call>) == (<reduce-exp>)
    ensures (|input| > 0 && input[0].0 != casper_key) ==> 
        doreduce(input, casper_key<reducer-args-call>) == doreduce(input[1..], casper_key<reducer-args-call>)
{
    if input == [] then (<reduce-init-value> )
    else if input[0].0 == casper_key then (<reduce-exp>)
    else doreduce(input[1..], casper_key<reducer-args-call>)
}  

<reduce-exp-lemma>