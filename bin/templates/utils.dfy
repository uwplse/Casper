/***************************** UTIL Functions *******************************/
function casper_str_equal(val1: int, val2: int) : bool
{
  val1 == val2
}

function casper_math_sqrt(val: int) : int
	requires val >= 0
{
	casper_math_sqrt2(val,0)
}

function casper_math_sqrt2(val: int, i: int) : int
	requires val >= 0
{
	if i*i < val then casper_math_sqrt2(val,i+1)
	else i
}

function casper_math_abs(val: int): int
{
	if val < 0 then val*-1
	else val
}

function casper_math_min(val1: int, val2: int): int
{
	if val1 < val2 then val1
	else val2
}

function casper_math_max(val1: int, val2: int): int
{
	if val1 < val2 then val2
	else val1
}