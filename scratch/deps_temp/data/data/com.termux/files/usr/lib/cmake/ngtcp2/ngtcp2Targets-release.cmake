#----------------------------------------------------------------
# Generated CMake target import file for configuration "Release".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "ngtcp2::ngtcp2" for configuration "Release"
set_property(TARGET ngtcp2::ngtcp2 APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(ngtcp2::ngtcp2 PROPERTIES
  IMPORTED_LOCATION_RELEASE "/data/data/com.termux/files/usr/lib/libngtcp2.so"
  IMPORTED_SONAME_RELEASE "libngtcp2.so"
  )

list(APPEND _cmake_import_check_targets ngtcp2::ngtcp2 )
list(APPEND _cmake_import_check_files_for_ngtcp2::ngtcp2 "/data/data/com.termux/files/usr/lib/libngtcp2.so" )

# Import target "ngtcp2::ngtcp2_static" for configuration "Release"
set_property(TARGET ngtcp2::ngtcp2_static APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(ngtcp2::ngtcp2_static PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "C"
  IMPORTED_LOCATION_RELEASE "/data/data/com.termux/files/usr/lib/libngtcp2.a"
  )

list(APPEND _cmake_import_check_targets ngtcp2::ngtcp2_static )
list(APPEND _cmake_import_check_files_for_ngtcp2::ngtcp2_static "/data/data/com.termux/files/usr/lib/libngtcp2.a" )

# Import target "ngtcp2::ngtcp2_crypto_ossl" for configuration "Release"
set_property(TARGET ngtcp2::ngtcp2_crypto_ossl APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(ngtcp2::ngtcp2_crypto_ossl PROPERTIES
  IMPORTED_LOCATION_RELEASE "/data/data/com.termux/files/usr/lib/libngtcp2_crypto_ossl.so"
  IMPORTED_SONAME_RELEASE "libngtcp2_crypto_ossl.so"
  )

list(APPEND _cmake_import_check_targets ngtcp2::ngtcp2_crypto_ossl )
list(APPEND _cmake_import_check_files_for_ngtcp2::ngtcp2_crypto_ossl "/data/data/com.termux/files/usr/lib/libngtcp2_crypto_ossl.so" )

# Import target "ngtcp2::ngtcp2_crypto_ossl_static" for configuration "Release"
set_property(TARGET ngtcp2::ngtcp2_crypto_ossl_static APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(ngtcp2::ngtcp2_crypto_ossl_static PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_RELEASE "C"
  IMPORTED_LOCATION_RELEASE "/data/data/com.termux/files/usr/lib/libngtcp2_crypto_ossl.a"
  )

list(APPEND _cmake_import_check_targets ngtcp2::ngtcp2_crypto_ossl_static )
list(APPEND _cmake_import_check_files_for_ngtcp2::ngtcp2_crypto_ossl_static "/data/data/com.termux/files/usr/lib/libngtcp2_crypto_ossl.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
