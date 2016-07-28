/***************************** DO MAP ***************************************/
function domap (<mapper-args-decl>) : <domap-emit-type>
  <loop-counter-range-domap>
  ensures domap(<mapper-args-call>) == <domap-emits>
{
  <domap-emits>
}

/***************************** MAPPER ***************************************/

function mapper (<mapper-args-decl>) : <domap-emit-type>
  <loop-counter-range-map>
{
  if <terminate-condition> then []
  else domap(<mapper-args-call-inductive>) + mapper(<mapper-args-call-inductive>)
}

/***************************** DO REDUCE ************************************/

function doreduce(input: <domap-emit-type>, key: <doreduce-key-type>) : <output-type>
  ensures (|input| > 0 && input[0].0 == key) ==> 
      doreduce(input, key) == <reduce-exp>
  ensures (|input| > 0 && input[0].0 != key) ==> 
      doreduce(input, key) == doreduce(input[1..], key)
{
  if input == [] then <reduce-init-value> 
  else if input[0].0 == key then <reduce-exp>
  else doreduce(input[1..], key)
}

/******************************* HARNESS ************************************/  

lemma Lemma2 (a: <domap-emit-type>, b: <domap-emit-type>, key: <doreduce-key-type>)
  ensures doreduce(a+b,key) == (doreduce(a,key) + doreduce(b,key))
{
  if a != []
  {
    Lemma2(a[1..], b, key);
    assert a + b == [a[0]] + (a[1..] + b);
  }
}

lemma Lemma (<inv-pc-args>)
  <lemma-requires>
  requires <invariant>
  
{
  assert mapper(<mapper-args-call-inductive-2>) == domap(<mapper-args-call>) + mapper(<mapper-args-call>);

  <emit-lemmas>
}

predicate loopInvariant (<inv-pc-args>)
  <inv-requires>
{
  <loop-inv>
}

predicate postCondition (<inv-pc-args>)
  <pcond-requires>
{
  <post-cond>
}

method harness (<harness-args>)
  <main-requires>
{
  <init-vars>
  <verif-code>
}