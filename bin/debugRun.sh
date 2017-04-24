BASEDIR=$(dirname "$0")
INPUTDIR=$(readlink -f "$1")
OUTPUTDIR=$(readlink -f "$2")
cd $BASEDIR
cd ..
ant
cd bin
./casperc -nooutput -stdout -w 10000 $INPUTDIR slow > $OUTPUTDIR
nodejs indentationScript.js $OUTPUTDIR
