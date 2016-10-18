/***************************** UTIL Functions *******************************/
function str_equal(val1: int, val2: int) : bool
{
  val1 == val2
}

function shift_right(val: seq<bool>, amnt: int) : seq<bool>
	requires |val| == 32
	requires 0 <= amnt <= 32
	ensures shift_right(val,amnt) == (zeroSeq(amnt) + val[0..32-amnt])
	ensures |shift_right(val,amnt)| == 32
{
	zeroSeq(amnt) + val[0..32-amnt]
}

function shift_left(val: seq<bool>, amnt: int) : seq<bool>
	requires |val| == 32
	requires 0 <= amnt <= 32
	ensures shift_left(val,amnt) == (val[amnt..32] + zeroSeq(amnt))
	ensures |shift_left(val,amnt)| == 32
{
	val[amnt..32] + zeroSeq(amnt)
}

function zeroSeq(amnt: int) : seq<bool>
	requires amnt >= 0
	ensures (amnt == 0) ==> (zeroSeq(amnt) == [])
	ensures (amnt > 0) ==> (zeroSeq(amnt) == [false] + zeroSeq(amnt-1))
	ensures |zeroSeq(amnt)| == amnt
{
	if amnt == 0 then []
	else [false] + zeroSeq(amnt-1)
}

function bitwise_and(val1: seq<bool>, val2: seq<bool>) : seq<bool>
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

function bitwise_or(val1: seq<bool>, val2: seq<bool>) : seq<bool>
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