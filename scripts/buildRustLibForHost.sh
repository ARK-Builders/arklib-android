#!/usr/bin/env bash

host_arch=$(rustc -vV | sed -n 's|host: ||p')
cd ../arklib
cargo build --target $host_arch

# Linux
if [ -e "target/$host_arch/debug/libarklib.so" ]; then
    lib_file="target/$host_arch/debug/libarklib.so" 
fi

# Mac
if [ -e "target/$host_arch/debug/libarklib.dylib" ]; then
    lib_file="target/$host_arch/debug/libarklib.dylib"
fi

# .dll for Windows

cp $lib_file ../

