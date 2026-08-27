cmake_minimum_required(VERSION 3.22.1)
project(structural_occt LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(OCCT_INC "${CMAKE_CURRENT_SOURCE_DIR}/occt/include/opencascade")
set(OCCT_LIB "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")

if(NOT EXISTS "${OCCT_INC}/Standard.hxx")
    message(FATAL_ERROR "OCCT headers not staged. CI must build/stage OCCT before Gradle.")
endif()

add_library(structural_occt SHARED structural_occt.cpp)
target_include_directories(structural_occt PRIVATE "${OCCT_INC}")

set(OCCT_LIBS
    TKernel TKMath TKG2d TKG3d TKBRep TKGeomBase TKGeomAlgo
    TKTopAlgo TKMesh TKShHealing TKPrim TKBO TKXSBase TKDESTEP TKDEIGES
)

foreach(L IN LISTS OCCT_LIBS)
    add_library(${L} SHARED IMPORTED)
    set_target_properties(${L} PROPERTIES IMPORTED_LOCATION "${OCCT_LIB}/lib${L}.so")
endforeach()

target_link_libraries(structural_occt PRIVATE ${OCCT_LIBS} log android)
