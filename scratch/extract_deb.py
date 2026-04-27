import os

def extract_deb(deb_file):
    with open(deb_file, 'rb') as f:
        magic = f.read(8)
        if magic != b'!<arch>\n':
            print("Not a valid deb file")
            return
            
        while True:
            header = f.read(60)
            if len(header) < 60:
                break
            
            name = header[:16].strip().decode().rstrip('/')
            size = int(header[48:58].strip())
            
            print(f"Found entry: {name}, size: {size}")
            
            content = f.read(size)
            if size % 2 != 0:
                f.read(1)
                
            if name.startswith('data.tar'):
                with open(name, 'wb') as out:
                    out.write(content)
                print(f"Extracted {name}")
                return name

if __name__ == "__main__":
    extract_deb("libgit2.deb")
