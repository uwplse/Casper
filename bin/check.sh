#!/bin/bash

function require-program {
    echo -n "checking for $1... "
    if ! which $1; then
        echo "MISSING!"
        return 1
    fi
}

require-program java;
if [[ $? == 0 ]]; then
    echo "FOUND!"
fi

require-program nodejs;
if [[ $? == 0 ]]; then
    echo "FOUND!"
fi

require-program dafny;
if [[ $? == 0 ]]; then
    echo "FOUND!"
fi

require-program sketch;
if [[ $? == 0 ]]; then
    echo "FOUND!"
fi

require-program ant;
if [[ $? == 0 ]]; then
    echo "FOUND!"
fi