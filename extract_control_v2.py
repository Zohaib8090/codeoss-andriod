import os

def extract_control(deb_path, out_path):
    with open(deb_path, 'rb') as f:
        if f.read(8) != b'!<arch>\n':
            return False
        while True:
            header = f.read(60)
            if not header or len(header) < 60:
                break
            name = header[0:16].decode().strip()
            size = int(header[48:58].decode().strip())
            if name.startswith('control.tar.xz'):
                with open(out_path, 'wb') as out:
                    out.write(f.read(size))
                return True
            else:
                f.seek(size, 1)
                if size % 2 != 0:
                    f.read(1)
    return False

extract_control('dl/git.deb', 'dl/control_git.tar.xz')
extract_control('dl/nodejs.deb', 'dl/control_node.tar.xz')
