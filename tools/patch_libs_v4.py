import os
import subprocess
import shutil

PATCHELF = r"C:\Users\zohai\Downloads\patchelf-win64-0.15.5.exe"
JNI_DIR = r"app\src\main\jniLibs\arm64-v8a"

RENAME_MAP = {
    "libcrypto.so.3": "libcrypt3.so",
    "libssl.so.3": "libsl3.so",
    "libz.so.1": "libz1.so",
    "libz.so": "libz1.so",
    "libicui18n.so.78": "libi18n78.so",
    "libicuuc.so.78": "libicu78.so",
    "libicudata.so.78": "libicud78.so",
    "libsqlite3.so": "libsqli3.so",
    "libcurl.so": "libcurl4.so",
    "libssh2.so": "libssh21.so",
    "libpcre2-8.so": "libpcre28.so",
    "libiconv.so": "libiconv1.so",
    "libexpat.so": "libexpat1.so",
    "libcares.so": "libcare.so",
    "libnghttp2.so": "libngh2.so",
    "libnghttp3.so": "libngh3.so",
    "libngtcp2.so": "libngtp2.so",
    "libbrotlicommon.so": "libbrcm.so",
    "libbrotlidec.so": "libbrdec.so",
    "libbrotlienc.so": "libbrenc.so"
}

# 1. Rename files
print("Renaming files...")
for filename in os.listdir(JNI_DIR):
    if filename in RENAME_MAP:
        src = os.path.join(JNI_DIR, filename)
        dst = os.path.join(JNI_DIR, RENAME_MAP[filename])
        if os.path.exists(dst): os.remove(dst)
        os.rename(src, dst)

# 2. Patch references
print("Patching references...")
for filename in os.listdir(JNI_DIR):
    if not filename.endswith(".so"): continue
    filepath = os.path.join(JNI_DIR, filename)
    
    # Use --no-sort for libnode.so to avoid hash corruption
    extra_args = ["--no-sort"] if filename == "libnode.so" else []
    
    # Set SONAME
    if filename in RENAME_MAP.values():
        subprocess.run([PATCHELF] + extra_args + ["--set-soname", filename, filepath], capture_output=True)

    # Replace needed
    for old_name, new_name in RENAME_MAP.items():
        subprocess.run([PATCHELF] + extra_args + ["--replace-needed", old_name, new_name, filepath], capture_output=True)

print("Patching Complete.")
