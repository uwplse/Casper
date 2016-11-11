/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
<udts>

/***************************** DO MAP ***************************************/

<emit-funcs>
function domap (<mapper-args-decl>) : <domap-emit-type>
    <emit-requires>
    ensures domap(<mapper-args-call>) == <domap-emits>
{
    <domap-emits>
}

/***************************** MAPPER ***************************************/

function mapper (<mapper-args-decl>) : <domap-emit-type>
    <mapper-requires>
{
    if <terminate-condition> then []
    else domap(<mapper-args-call-inductive>) + mapper(<mapper-args-call-inductive>)
}

/***************************** DO REDUCE ************************************/
<reduce-functions>

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

predicate loopInvariant (<inv-pc-args>)
    <inv-requires>
{
    <loop-inv>
}

predicate postCondition (<inv-pc-args>)
    <inv-requires>
{
    <post-cond>
}

method harness (<harness-args>)
    <main-requires>
{
    <init-vars>
    <verif-code>
}