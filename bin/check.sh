#!/bin/bash

function require-program {
    echo -n "checking for $1... "
    if ! which $1; then
        err "missing!"
        return 1
    fi
}

require-program nodejs;
require-program dafny;
require-program sketch;

if [[ $? == 0 ]]; then
    echo "SUCCESS!"
else
    exit 1
fi