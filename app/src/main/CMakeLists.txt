cmake_minimum_required(VERSION 3.4.1)

# Add the C source files for Dilithium
add_library(
    native-lib
    SHARED
    cpp/native-lib.cpp
    cpp/src/fips202.c
    cpp/src/ntt.c
    cpp/src/packing.c
    cpp/src/poly.c
    cpp/src/polyvec.c
    cpp/src/randombytes.c
    cpp/src/reduce.c
    cpp/src/rounding.c
    cpp/src/sign.c
    cpp/src/symmetric-shake.c
    cpp/src/aes.c
)

# Include the headers
include_directories(cpp/include)

# Link the libraries
find_library(
    log-lib
    log
)

target_link_libraries(
    native-lib
    ${log-lib}
)