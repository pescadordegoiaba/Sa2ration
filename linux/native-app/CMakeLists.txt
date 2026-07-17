cmake_minimum_required(VERSION 3.24)

project(sa2ration-linux-native VERSION 2.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 6.6 REQUIRED COMPONENTS Core DBus Gui OpenGL OpenGLWidgets Widgets)

set(IMGUI_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../third_party/imgui")

add_library(sa2ration_imgui STATIC
    "${IMGUI_DIR}/imgui.cpp"
    "${IMGUI_DIR}/imgui_draw.cpp"
    "${IMGUI_DIR}/imgui_tables.cpp"
    "${IMGUI_DIR}/imgui_widgets.cpp"
    "${IMGUI_DIR}/backends/imgui_impl_opengl3.cpp"
)
target_include_directories(sa2ration_imgui PUBLIC
    "${IMGUI_DIR}"
    "${IMGUI_DIR}/backends"
)
target_link_libraries(sa2ration_imgui PUBLIC Qt6::OpenGL)

qt_add_executable(sa2ration-linux
    main.cpp
    gpueffectcontroller.cpp
    gpueffectcontroller.h
    imguiwidget.cpp
    imguiwidget.h
    mainwindow.cpp
    mainwindow.h
)

target_link_libraries(sa2ration-linux PRIVATE
    sa2ration_imgui
    Qt6::Core
    Qt6::DBus
    Qt6::Gui
    Qt6::OpenGL
    Qt6::OpenGLWidgets
    Qt6::Widgets
)

target_compile_options(sa2ration-linux PRIVATE
    $<$<CXX_COMPILER_ID:GNU,Clang>:-Wall -Wextra -Wpedantic -Wconversion>
)

install(TARGETS sa2ration-linux RUNTIME DESTINATION bin)
