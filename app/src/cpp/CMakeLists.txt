cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")

add_library(tinyGps SHARED
           TinyGPS++.cpp
           TinyGPS++.h)

# Include libraries needed for plasma lib
target_link_libraries(tinyGps
                      android
                      jnigraphics
                      log
m)