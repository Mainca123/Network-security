cmake_minimum_required(VERSION 3.16)

project(DSA_System LANGUAGES CXX)

# =========================================================
# C++ Standard
# =========================================================
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# =========================================================
# QT & Meta-compilers
# =========================================================
find_package(Qt6 REQUIRED COMPONENTS Widgets Pdf)

qt_standard_project_setup()

# Bật tính năng tự động xử lý Signal/Slot và File UI của Qt
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTOUIC ON)

# =========================================================
# OpenSSL (Configured for MSYS2 UCRT64)
# =========================================================
set(OPENSSL_ROOT_DIR "C:/msys64/ucrt64")
find_package(OpenSSL REQUIRED)

# =========================================================
# AUTOUIC SEARCH PATHS
# =========================================================
# Qt tự tìm file .ui trong thư mục forms
set(CMAKE_AUTOUIC_SEARCH_PATHS forms)

# =========================================================
# UI FILES
# =========================================================
set(UI_FILES
    forms/mainwindow.ui
)

# =========================================================
# SOURCE FILES
# =========================================================
set(SOURCES
    # Main
    src/main.cpp
    src/mainwindow.cpp

    # Crypto
    src/crypto/dsa_manager.cpp
    src/crypto/hash_utils.cpp
    src/crypto/key_generator.cpp
    src/crypto/dsa_handmade.cpp


    # File IO
    src/file_io/file_signer.cpp
    src/file_io/file_renderer.cpp

    # Key Storage
    src/key_storage/key_manager.cpp

    # Logging
    src/logging/logger.cpp
)

# =========================================================
# HEADER FILES
# =========================================================
set(HEADERS
    # Main
    src/mainwindow.h

    # Crypto
    src/crypto/dsa_manager.h
    src/crypto/hash_utils.h
    src/crypto/key_generator.h
    src/crypto/dsa_handmade.h

    # File IO
    src/file_io/file_signer.h
    src/file_io/file_renderer.h

    # Key Storage
    src/key_storage/key_manager.h

    # Logging
    src/logging/logger.h
)

# =========================================================
# EXECUTABLE
# =========================================================
qt_add_executable(
    DSA_System
    ${SOURCES}
    ${HEADERS}
    ${UI_FILES}
    app.rc
)

qt_add_resources(DSA_System "Styles"
    PREFIX "/"
    FILES
        forms/style_light.qss
        forms/style_dark.qss
)

# =========================================================
# INCLUDE DIRECTORIES
# =========================================================
target_include_directories(DSA_System PRIVATE
    src
    src/crypto
    src/file_io
    src/key_storage
    src/logging
    ${OPENSSL_INCLUDE_DIR}
)

# =========================================================
# LIBRARIES
# =========================================================
target_link_libraries(DSA_System PRIVATE
    Qt6::Widgets
    Qt6::Pdf
    OpenSSL::SSL
    OpenSSL::Crypto
)

# =========================================================
# PROJECT TREE (IDE Display)
# =========================================================
source_group(TREE ${CMAKE_CURRENT_SOURCE_DIR} FILES
    ${SOURCES}
    ${HEADERS}
    ${UI_FILES}
)