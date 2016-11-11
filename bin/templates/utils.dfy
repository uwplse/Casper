/***************************** UTIL Functions *******************************/
function casper_str_equal(val1: int, val2: int) : bool
{
  val1 == val2
}

function casper_shift_right(val: seq<bool>, amnt: int) : seq<bool>
	requires |val| == 32
	requires 0 <= amnt <= 32
	ensures casper_shift_right(val,amnt) == (casper_zeroSeq(amnt) + val[0..32-amnt])
	ensures |casper_shift_right(val,amnt)| == 32
{
	casper_zeroSeq(amnt) + val[0..32-amnt]
}

function casper_shift_left(val: seq<bool>, amnt: int) : seq<bool>
	requires |val| == 32
	requires 0 <= amnt <= 32
	ensures casper_shift_left(val,amnt) == (val[amnt..32] + casper_zeroSeq(amnt))
	ensures |casper_shift_left(val,amnt)| == 32
{
	val[amnt..32] + casper_zeroSeq(amnt)
}

function casper_zeroSeq(amnt: int) : seq<bool>
	requires amnt >= 0
	ensures (amnt == 0) ==> (casper_zeroSeq(amnt) == [])
	ensures (amnt > 0) ==> (casper_zeroSeq(amnt) == [false] + casper_zeroSeq(amnt-1))
	ensures |casper_zeroSeq(amnt)| == amnt
{
	if amnt == 0 then []
	else [false] + casper_zeroSeq(amnt-1)
}

function casper_bitwise_and(val1: seq<bool>, val2: seq<bool>) : seq<bool>
	requires |val1| == 32
	requires |val2| == 32
{
	[
		val1[0] && val2[0],
		val1[1] && val2[1],
		val1[2] && val2[2],
		val1[3] && val2[3],
		val1[4] && val2[4],
		val1[5] && val2[5],
		val1[6] && val2[6],
		val1[7] && val2[7],
		val1[8] && val2[8],
		val1[9] && val2[9],
		val1[10] && val2[10],
		val1[11] && val2[11],
		val1[12] && val2[12],
		val1[13] && val2[13],
		val1[14] && val2[14],
		val1[15] && val2[15],
		val1[16] && val2[16],
		val1[17] && val2[17],
		val1[18] && val2[18],
		val1[19] && val2[19],
		val1[20] && val2[20],
		val1[21] && val2[21],
		val1[22] && val2[22],
		val1[23] && val2[23],
		val1[24] && val2[24],
		val1[25] && val2[25],
		val1[26] && val2[26],
		val1[27] && val2[27],
		val1[28] && val2[28],
		val1[29] && val2[29],
		val1[30] && val2[30],
		val1[31] && val2[31]
	]
}

function casper_bitwise_or(val1: seq<bool>, val2: seq<bool>) : seq<bool>
	requires |val1| == 32
	requires |val2| == 32
{
	[
		val1[0] || val2[0],
		val1[1] || val2[1],
		val1[2] || val2[2],
		val1[3] || val2[3],
		val1[4] || val2[4],
		val1[5] || val2[5],
		val1[6] || val2[6],
		val1[7] || val2[7],
		val1[8] || val2[8],
		val1[9] || val2[9],
		val1[10] || val2[10],
		val1[11] || val2[11],
		val1[12] || val2[12],
		val1[13] || val2[13],
		val1[14] || val2[14],
		val1[15] || val2[15],
		val1[16] || val2[16],
		val1[17] || val2[17],
		val1[18] || val2[18],
		val1[19] || val2[19],
		val1[20] || val2[20],
		val1[21] || val2[21],
		val1[22] || val2[22],
		val1[23] || val2[23],
		val1[24] || val2[24],
		val1[25] || val2[25],
		val1[26] || val2[26],
		val1[27] || val2[27],
		val1[28] || val2[28],
		val1[29] || val2[29],
		val1[30] || val2[30],
		val1[31] || val2[31]
	]
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