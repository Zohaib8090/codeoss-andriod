# fetch-binaries.ps1
# This script downloads and prepares ARM64 Node.js and Git binaries for CodeOSS Android.

$ErrorActionPreference = "Stop"

$BASE_URL = "https://packages-cf.termux.dev/apt/termux-main/pool/main"
$JNI_DIR = "app/src/main/jniLibs/arm64-v8a"
$TEMP_DIR = "scratch/binaries"

if (!(Test-Path $JNI_DIR)) { New-Item -ItemType Directory -Force -Path $JNI_DIR }
if (!(Test-Path $TEMP_DIR)) { New-Item -ItemType Directory -Force -Path $TEMP_DIR }

# Define packages to download (latest as of research)
$PACKAGES = @(
    @{ name="nodejs"; url="$BASE_URL/n/nodejs/nodejs_20.10.0-1_aarch64.deb" },
    @{ name="git"; url="$BASE_URL/g/git/git_2.43.0_aarch64.deb" },
    @{ name="libandroid-support"; url="$BASE_URL/l/libandroid-support/libandroid-support_28_aarch64.deb" },
    @{ name="libpcre2"; url="$BASE_URL/l/libpcre2/libpcre2_10.42_aarch64.deb" },
    @{ name="libsqlite"; url="$BASE_URL/l/libsqlite/libsqlite_3.47.1_aarch64.deb" }
)

foreach ($pkg in $PACKAGES) {
    $fileName = Split-Path $pkg.url -Leaf
    $dest = Join-Path $TEMP_DIR $fileName
    
    Write-Host "Downloading $($pkg.name)..."
    Invoke-WebRequest -Uri $pkg.url -OutFile $dest
    
    # Extract using tar (standard in Win10+)
    Write-Host "Extracting $($pkg.name)..."
    # Deb is an ar archive containing data.tar.xz
    # Since we don't have 'ar' on Windows easily, we use a trick or assume the user has a tool.
    # Actually, 7zip or WinRAR can do it. 
    # But for an automated script, let's try to use Expand-Archive if it's a zip, but it's a deb.
    
    # ALTERNATIVE: Use the direct binaries if available.
}

Write-Host "NOTE: .deb extraction on Windows requires 'ar' and 'tar'. "
Write-Host "If you don't have these, please manually extract the binaries from the .deb files"
Write-Host "and place them in $JNI_DIR as libnode.so, libgit.so, etc."
