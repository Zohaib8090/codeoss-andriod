#!/usr/bin/env python3
"""
patch_runpath.py — CodeOSS Android RUNPATH Patcher
Replaces the hardcoded Termux RUNPATH in compiled ELF binaries with $ORIGIN
so they work standalone inside the APK without Termux installed.

Usage: python3 patch_runpath.py <path-to-binary>

The replacement is a precise 35-character hex patch:
  OLD (35 chars): /data/data/com.termux/files/usr/lib
  NEW (35 chars): $ORIGIN/./././././././././././././.

IMPORTANT: Only run this once per binary — running it twice will corrupt it.
"""

import sys
import os

OLD = b'/data/data/com.termux/files/usr/lib'
NEW = b'$ORIGIN/./././././././././././././.'

assert len(OLD) == 35, f"OLD length is {len(OLD)}, expected 35"
assert len(NEW) == 35, f"NEW length is {len(NEW)}, expected 35"

def patch(path: str) -> bool:
    with open(path, 'rb') as f:
        data = f.read()

    # Sanity: must be ELF
    if data[:4] != b'\x7fELF':
        print(f"ERROR: {path} is not an ELF binary")
        sys.exit(1)

    count = data.count(OLD)
    if count == 0:
        # Check if already patched
        if data.count(NEW) > 0:
            print(f"SKIP: {path} is already patched ($ORIGIN found)")
            return False
        print(f"WARNING: RUNPATH string not found in {path}.")
        print("         This binary may not have a Termux RUNPATH, or it uses a different path.")
        return False

    patched = data.replace(OLD, NEW)

    # Verify the replacement didn't change file size (critical for ELF validity)
    assert len(patched) == len(data), "File size changed after patching — this should never happen!"

    with open(path, 'wb') as f:
        f.write(patched)

    print(f"OK: Patched {count} occurrence(s) in {path}")
    return True

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    
    for path in sys.argv[1:]:
        if not os.path.isfile(path):
            print(f"ERROR: File not found: {path}")
            sys.exit(1)
        patch(path)
