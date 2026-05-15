cmake_minimum_required(VERSION 3.22.1)

project("codeossandriod")

# Include directories for libgit2 headers
include_directories(include)

# Import libgit2
add_library(git2 SHARED IMPORTED)
set_target_properties(git2 PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libgit2.so)

# Import dependencies
add_library(curl SHARED IMPORTED)
set_target_properties(curl PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libcurl.so)

add_library(ssl SHARED IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libssl.so)

add_library(crypto SHARED IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libcrypto.so)

add_library(ssh2 SHARED IMPORTED)
set_target_properties(ssh2 PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libssh2.so)

add_library(
    native-lib
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    native-lib
    git2
    curl
    ssl
    crypto
    ssh2
    ${log-lib}
    android
)
