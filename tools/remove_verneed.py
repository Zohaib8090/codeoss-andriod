import struct

def remove_verneed(filepath):
    with open(filepath, 'rb+') as f:
        # Read ELF header (64-bit)
        f.seek(0)
        e_ident = f.read(16)
        if e_ident[:4] != b'\x7fELF':
            print("Not an ELF file")
            return
        
        # We need e_phoff, e_phentsize, e_phnum
        f.seek(32)
        e_phoff = struct.unpack('<Q', f.read(8))[0]
        
        f.seek(54)
        e_phentsize = struct.unpack('<H', f.read(2))[0]
        e_phnum = struct.unpack('<H', f.read(2))[0]
        
        dynamic_offset = 0
        dynamic_size = 0
        
        # Find PT_DYNAMIC segment
        for i in range(e_phnum):
            f.seek(e_phoff + i * e_phentsize)
            p_type = struct.unpack('<I', f.read(4))[0]
            if p_type == 2: # PT_DYNAMIC
                f.seek(e_phoff + i * e_phentsize + 8)
                dynamic_offset = struct.unpack('<Q', f.read(8))[0]
                f.seek(e_phoff + i * e_phentsize + 32)
                dynamic_size = struct.unpack('<Q', f.read(8))[0]
                break
                
        if dynamic_offset == 0:
            print("No PT_DYNAMIC found")
            return
            
        # Parse .dynamic section
        f.seek(dynamic_offset)
        print(f"Parsing .dynamic at offset {hex(dynamic_offset)}")
        num_entries = dynamic_size // 16
        
        for i in range(num_entries):
            f.seek(dynamic_offset + i * 16)
            d_tag = struct.unpack('<q', f.read(8))[0]
            if d_tag == 0x6ffffffe or d_tag == 0x6fffffff: # DT_VERNEED or DT_VERNEEDNUM
                print(f"Found version tag {hex(d_tag)} at index {i}, nullifying...")
                f.seek(dynamic_offset + i * 16)
                # Nullify the tag (DT_NULL = 0)
                f.write(struct.pack('<q', 0))
                f.write(struct.pack('<q', 0))
                
        print("Done patching VERNEED.")

remove_verneed("app/src/main/jniLibs/arm64-v8a/libnode.so")
