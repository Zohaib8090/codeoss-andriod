import os
import glob

def patch_binary(path, search, replace):
    if not os.path.exists(path):
        return False
    with open(path, 'rb') as f:
        content = f.read()
    
    if search.encode() in content:
        print(f"Found {search} in {path}, patching...")
        new_content = content.replace(search.encode(), replace.encode())
        with open(path, 'wb') as f:
            f.write(new_content)
        return True
    return False

lib_dir = r"app\src\main\jniLibs\arm64-v8a"
libgit2 = os.path.join(lib_dir, "libgit2.so")

# Patch the source libgit2.so
patch_binary(libgit2, "libssl.so.3", "libssl.so\0\0")
patch_binary(libgit2, "libcrypto.so.3", "libcrypto.so\0\0")
patch_binary(libgit2, "libssh2.so.1", "libssh2.so\0\0")

# Patch all libnative-lib.so files in build directory
for f in glob.glob(r"app\build\intermediates\**\libnative-lib.so", recursive=True):
    patch_binary(f, "libssl.so.3", "libssl.so\0\0")
    patch_binary(f, "libcrypto.so.3", "libcrypto.so\0\0")
    patch_binary(f, "libssh2.so.1", "libssh2.so\0\0")
