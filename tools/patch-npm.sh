#!/system/bin/sh
DNS_OVERRIDE="/data/user/0/com.kodrix.zohaib/files/usr/etc/dns-override.js"
INIT_SH="/data/user/0/com.kodrix.zohaib/files/init.sh"

# Rewrite the npm alias to include NODE_OPTIONS inline
LIBNODE="/data/app/~~9GcPo9Sy0YNcMDE3-dydHA==/com.kodrix.zohaib-6KEFPJkzn_JcgEYf_2CQxA==/lib/arm64/libnode.so"
LIBPATH="/data/app/~~9GcPo9Sy0YNcMDE3-dydHA==/com.kodrix.zohaib-6KEFPJkzn_JcgEYf_2CQxA==/lib/arm64:/data/user/0/com.kodrix.zohaib/files/lib_links"
NPM_CLI="/data/user/0/com.kodrix.zohaib/files/npm_pkg/bin/npm-cli.js"
NPX_CLI="/data/user/0/com.kodrix.zohaib/files/npm_pkg/bin/npx-cli.js"

# Remove old npm/npx aliases and add new ones with NODE_OPTIONS
grep -v "^alias npm=" "$INIT_SH" | grep -v "^alias npx=" | grep -v "^export NODE_OPTIONS=" > /data/user/0/com.kodrix.zohaib/files/init.sh.tmp

echo "export NODE_OPTIONS=\"--require $DNS_OVERRIDE\"" >> /data/user/0/com.kodrix.zohaib/files/init.sh.tmp
echo "alias npm='LD_LIBRARY_PATH=\"$LIBPATH\" NODE_OPTIONS=\"--require $DNS_OVERRIDE\" $LIBNODE $NPM_CLI'" >> /data/user/0/com.kodrix.zohaib/files/init.sh.tmp
echo "alias npx='LD_LIBRARY_PATH=\"$LIBPATH\" NODE_OPTIONS=\"--require $DNS_OVERRIDE\" $LIBNODE $NPX_CLI'" >> /data/user/0/com.kodrix.zohaib/files/init.sh.tmp

mv /data/user/0/com.kodrix.zohaib/files/init.sh.tmp "$INIT_SH"
echo "Done! init.sh patched."
cat "$INIT_SH" | grep -E "(npm|NODE)"
