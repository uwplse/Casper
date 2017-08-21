/***************************** INCLUDES *************************************/
include "utils.dfy"

/******************************* UDTS ***************************************/
<udts>

/***************************** DO MAP ***************************************/

<emit-funcs>

<do-map-functions>

/***************************** MAPPER ***************************************/

<mapper-functions>

/***************************** DO REDUCE ************************************/

<reduce-functions>

<do-reduce-functions>

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