cd /data/user/0/com.codeossandroid/files/projects/spoton
export HOME=/data/user/0/com.codeossandroid/files
export TMPDIR=/data/user/0/com.codeossandroid/files/tmp
export PATH=/data/user/0/com.codeossandroid/files/usr/bin:/system/bin
export LD_LIBRARY_PATH=/data/app/~~QdfUbAYvAb6hEQ-3zwU6ww==/com.codeossandroid-ryx1EnMpVqy5rYzOh6v1Cw==/lib/arm64/:/data/user/0/com.codeossandroid/files/usr/lib
export NODE_OPTIONS="--require /data/user/0/com.codeossandroid/files/usr/etc/dns-override.js"

echo "[DEBUG] LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
echo "[DEBUG] PATH=$PATH"

export OPENSSL_CONF=/dev/null
echo "[DEBUG] OPENSSL_CONF=$OPENSSL_CONF"
/data/user/0/com.codeossandroid/files/usr/bin/node napi_check.js node_modules/@next/swc-android-arm64/next-swc.android-arm64.node


