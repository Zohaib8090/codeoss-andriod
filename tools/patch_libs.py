import os

def patch_file(path, old_name, new_name):
    # Pad new_name with null bytes if shorter than old_name
    if len(new_name) < len(old_name):
        new_name = new_name + "\0" * (len(old_name) - len(new_name))
    elif len(new_name) > len(old_name):
        print(f"Error: {new_name} is longer than {old_name}!")
        return

    with open(path, 'rb') as f:
        data = f.read()
    
    old_bytes = old_name.encode('utf-8')
    new_bytes = new_name.encode('utf-8')
    
    count = data.count(old_bytes)
    if count > 0:
        print(f"FOUND {count} instances of '{old_name}' in {os.path.basename(path)}")
        new_data = data.replace(old_bytes, new_bytes)
        with open(path, 'wb') as f:
            f.write(new_data)
    else:
        print(f"NOT FOUND: '{old_name}' in {os.path.basename(path)}")

lib_dir = r"app/src/main/jniLibs/arm64-v8a"
for filename in os.listdir(lib_dir):
    if filename.endswith(".so"):
        path = os.path.join(lib_dir, filename)
        # OpenSSL versioned strings
        patch_file(path, "libcrypto.so.3", "libcrypt3.so")
        patch_file(path, "libssl.so.3", "libsl3.so")
        # Also cover any remaining .3 suffix after we renamed to libcrypt3.so
        patch_file(path, "libcrypt3.so.3", "libcrypt3.so")
        # Standard names (if any remain)
        patch_file(path, "libcrypto.so", "libcrypt3.so")
        patch_file(path, "libssl.so", "libsl3.so")
        # Version suffix fixes for other libs
        patch_file(path, "libz.so.1", "libz.so")
        patch_file(path, "libcurl.so.4", "libcurl.so")
        patch_file(path, "libssh2.so.1", "libssh2.so")
        patch_file(path, "libicui18n.so.78", "libicui18n.so")
        patch_file(path, "libicuuc.so.78", "libicuuc.so")
        patch_file(path, "libicudata.so.78", "libicudata.so")
