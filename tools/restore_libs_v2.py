import urllib.request
import os
import tarfile
import shutil

# Simple AR parser for .deb files
def extract_data_tar_xz(deb_path, out_path):
    with open(deb_path, 'rb') as f:
        if f.read(8) != b'!<arch>\n':
            return False
        while True:
            header = f.read(60)
            if not header or len(header) < 60:
                break
            name = header[0:16].decode().strip()
            size = int(header[48:58].decode().strip())
            if name.startswith('data.tar.xz'):
                with open(out_path, 'wb') as out:
                    out.write(f.read(size))
                return True
            else:
                f.seek(size, 1)
                if size % 2 != 0:
                    f.read(1)
    return False

def download_file(url, dest):
    print(f"Downloading {url}...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response, open(dest, 'wb') as out_file:
        shutil.copyfileobj(response, out_file)

BASE_URL = "https://packages.termux.org/apt/termux-main/pool/main"

PACKAGES = [
    {"name": "libnghttp2", "url": f"{BASE_URL}/libn/libnghttp2/libnghttp2_1.69.0_aarch64.deb"},
    {"name": "libnghttp3", "url": f"{BASE_URL}/libn/libnghttp3/libnghttp3_1.15.0_aarch64.deb"},
    {"name": "libngtcp2", "url": f"{BASE_URL}/libn/libngtcp2/libngtcp2_1.22.1_aarch64.deb"},
    {"name": "brotli", "url": f"{BASE_URL}/b/brotli/brotli_1.2.0_aarch64.deb"}
]

DL_DIR = "dl"
JNI_DIR = "app/src/main/jniLibs/arm64-v8a"

os.makedirs(DL_DIR, exist_ok=True)
os.makedirs(JNI_DIR, exist_ok=True)

for pkg in PACKAGES:
    pkg_name = pkg["name"]
    deb_path = os.path.join(DL_DIR, f"{pkg_name}.deb")
    data_tar_path = os.path.join(DL_DIR, f"{pkg_name}_data.tar.xz")
    
    try:
        download_file(pkg["url"], deb_path)
    except Exception as e:
        print(f"Failed to download {pkg_name}: {e}")
        continue
        
    print(f"Extracting {pkg_name}...")
    if extract_data_tar_xz(deb_path, data_tar_path):
        with tarfile.open(data_tar_path, "r:xz") as tar:
            for member in tar.getmembers():
                filename = os.path.basename(member.name)
                if filename.endswith(".so") or ".so." in filename:
                    target_path = os.path.join(JNI_DIR, filename)
                    f = tar.extractfile(member)
                    if f:
                        with open(target_path, "wb") as out:
                            out.write(f.read())
                        print(f" Saved {filename}")
    else:
        print(f" Could not find data.tar.xz in {pkg_name}")

print("\nRestoration Complete.")
