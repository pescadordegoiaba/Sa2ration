cmake_minimum_required(VERSION 3.24)

project(sa2ration_gpu_effect VERSION 2.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(ECM 6.26 REQUIRED NO_MODULE)
list(APPEND CMAKE_MODULE_PATH "${ECM_MODULE_PATH}")

include(KDEInstallDirs6)
include(KDECMakeSettings)
include(KDECompilerSettings NO_POLICY_SCOPE)

find_package(Qt6 6.10 REQUIRED COMPONENTS Core DBus Gui Quick Widgets)
find_package(KF6 6.26 REQUIRED COMPONENTS Config CoreAddons)
find_package(KWin 6.7 REQUIRED)

kcoreaddons_add_plugin(sa2ration_gpu
    SOURCES
        main.cpp
        sa2rationgpueffect.cpp
        sa2rationgpueffect.qrc
    INSTALL_NAMESPACE "kwin/effects/plugins"
)
set_property(TARGET sa2ration_gpu APPEND PROPERTY AUTOMOC_MACRO_NAMES "KWIN_EFFECT_FACTORY_SUPPORTED")

target_link_libraries(sa2ration_gpu PRIVATE
    KWin::kwin
    KF6::ConfigCore
)
