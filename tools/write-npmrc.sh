#!/system/bin/sh
# Write .npmrc with IP-based registry to bypass DNS entirely
NPMRC="/data/user/0/com.codeossandroid/files/projects/test/Spoton-web/.npmrc"

cat > "$NPMRC" << 'EOF'
registry=https://registry.npmjs.org/
strict-ssl=false
fetch-retry-mintimeout=20000
fetch-retry-maxtimeout=120000
EOF

echo ".npmrc written:"
cat "$NPMRC"
