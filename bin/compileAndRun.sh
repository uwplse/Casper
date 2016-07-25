cd ..
ant
cd bin
./casperc -nooutput -stdout -w 10000 $1 > $2
node indentationScript.js $2