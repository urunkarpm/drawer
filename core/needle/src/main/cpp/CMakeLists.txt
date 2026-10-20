cmake_minimum_required(VERSION 3.22.1)
project("needle_jni")

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

add_library(needle_engine STATIC IMPORTED)
set_target_properties(needle_engine PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libneedle.a
)

add_library(needle_jni SHARED
    needle_jni.cpp
)

target_link_libraries(needle_jni
    needle_engine
    log
)
