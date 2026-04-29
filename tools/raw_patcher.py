import os

def raw_patch(file_path, old_name, new_name):
    if len(old_name) != len(new_name):
        return False
    with open(file_path, 'rb') as f:
        data = f.read()
    old_bytes = old_name.encode('ascii')
    new_bytes = new_name.encode('ascii')
    if old_bytes not in data:
        return False
    new_data = data.replace(old_bytes, new_bytes)
    with open(file_path, 'wb') as f:
        f.write(new_data)
    print(f" Patched {old_name} -> {new_name} in {os.path.basename(file_path)}")
    return True

JNI_DIR = r"app\src\main\jniLibs\arm64-v8a"

RENAME_MAP = {
    "libz.so.1": "libz_9.so",        # 9 vs 9
    "libcrypto.so.3": "libcrypto_9.so", # 14 vs 14
    "libssl.so.3": "libssl_9.so",      # 11 vs 11
    "libsqlite3.so": "libsqlite9.so",  # 13 vs 13
    "libicui18n.so.78": "libicui18n_99.so", # 16 vs 16
    "libicuuc.so.78": "libicuuc_99.so",    # 14 vs 14
    "libicudata.so.78": "libicudata_99.so", # 16 vs 16
    "libcurl.so": "libcur9.so",        # 10 vs 10
    "libssh2.so": "libssh9.so"         # 10 vs 10
}

# 1. Rename files
print("Renaming files...")
for old, new in RENAME_MAP.items():
    found = False
    for filename in os.listdir(JNI_DIR):
        if filename == old or filename.startswith(old + "."):
            src = os.path.join(JNI_DIR, filename)
            dst = os.path.join(JNI_DIR, new)
            if os.path.exists(dst): os.remove(dst)
            os.rename(src, dst)
            print(f" File {filename} -> {new}")
            found = True
            break

# 2. Patch all binaries
print("\nPatching all binaries...")
for filename in os.listdir(JNI_DIR):
    if not filename.endswith(".so"): continue
    path = os.path.join(JNI_DIR, filename)
    for old, new in RENAME_MAP.items():
        raw_patch(path, old, new)

print("\nRaw Patching Complete.")
