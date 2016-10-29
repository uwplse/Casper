function doreduce(input: seq<(int, (int,int))>, key: int, loop0: bool) : int
    ensures (|input| > 0 && input[0].0 == key) ==> 
        doreduce(input, key, loop0) == (if key == 1 then ((doreduce(input[1..], key, loop0)+1)) else ((doreduce(input[1..], key, loop0)+1)))
    ensures (|input| > 0 && input[0].0 != key) ==> 
        doreduce(input, key, loop0) == doreduce(input[1..], key, loop0)
{
    if input == [] then (if key == 1 then 0 else 0 )
    else if input[0].0 == key then (if key == 1 then ((doreduce(input[1..], key, loop0)+1)) else ((doreduce(input[1..], key, loop0)+1)))
    else doreduce(input[1..], key, loop0)
}

/******************************* HARNESS ************************************/    

lemma LemmaCSG_count (a: seq<(int, (int,int))>, b: seq<(int, (int,int))>, key: int, loop0: bool)
	requires key == 1
ensures doreduce(a+b, key, loop0) == (doreduce(a, key, loop0) + doreduce(b, key, loop0))
{
	if a != []
	{
		LemmaCSG_count(a[1..], b, key, loop0);
		assert a + b == [a[0]] + (a[1..] + b);
	}
}

