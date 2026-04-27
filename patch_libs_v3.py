import os
import subprocess
import shutil

PATCHELF = r"C:\Users\zohai\Downloads\patchelf-win64-0.15.5.exe"
JNI_DIR = r"app\src\main\jniLibs\arm64-v8a"

# Mapping from original name (in DT_NEEDED) to new unique name
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

# 1. Rename the actual files
print("Renaming library files...")
files = os.listdir(JNI_DIR)
for filename in files:
    if filename in RENAME_MAP:
        src = os.path.join(JNI_DIR, filename)
        dst = os.path.join(JNI_DIR, RENAME_MAP[filename])
        if os.path.exists(dst): os.remove(dst)
        os.rename(src, dst)
        print(f" Renamed {filename} -> {RENAME_MAP[filename]}")
    elif ".so." in filename:
        base = filename
        while "." in base and not base.endswith(".so"):
            base = base.rsplit(".", 1)[0]
        if base in RENAME_MAP:
            src = os.path.join(JNI_DIR, filename)
            dst = os.path.join(JNI_DIR, RENAME_MAP[base])
            if os.path.exists(dst): os.remove(dst)
            os.rename(src, dst)
            print(f" Renamed {filename} -> {RENAME_MAP[base]}")

# 2. Update DT_NEEDED and SONAME in all binaries
print("\nUpdating internal references using patchelf...")
new_files = [f for f in os.listdir(JNI_DIR) if f.endswith(".so")]
for filename in new_files:
    filepath = os.path.join(JNI_DIR, filename)
    
    # Update SONAME
    try:
        if filename in RENAME_MAP.values():
            subprocess.run([PATCHELF, "--set-soname", filename, filepath], check=True)
    except Exception as e:
        pass

    # Update DT_NEEDED
    for old_name, new_name in RENAME_MAP.items():
        try:
            subprocess.run([PATCHELF, "--replace-needed", old_name, new_name, filepath], capture_output=True)
        except Exception as e:
            pass

print("\nPatching Complete.")
