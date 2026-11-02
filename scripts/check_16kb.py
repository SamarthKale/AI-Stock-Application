"""Check that every 64-bit native library in an APK is 16 KB page-size compatible.

Android 15+ devices running with 16 KB pages show "This app isn't 16 KB compatible" unless, for
each lib/<abi>/*.so in the APK:
  1. every ELF PT_LOAD segment is aligned to >= 16384, and
  2. the file is stored uncompressed at a zip data offset that is a multiple of 16384.

Usage:  python -I scripts/check_16kb.py path/to/app.apk
Exit code 0 = all good, 1 = at least one library fails.
"""
import struct
import sys
import zipfile

PAGE = 16384
PT_LOAD = 1
CHECKED_ABIS = ("lib/arm64-v8a/", "lib/x86_64/")  # 32-bit ABIs are exempt from the 16 KB rule


def min_load_alignment(data: bytes) -> int:
    if data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    is64 = data[4] == 2
    if is64:
        phoff = struct.unpack_from("<Q", data, 0x20)[0]
        phentsize, phnum = struct.unpack_from("<HH", data, 0x36)
        align_off = 0x30
        align_fmt = "<Q"
    else:
        phoff = struct.unpack_from("<I", data, 0x1C)[0]
        phentsize, phnum = struct.unpack_from("<HH", data, 0x2A)
        align_off = 0x1C
        align_fmt = "<I"
    aligns = []
    for k in range(phnum):
        base = phoff + k * phentsize
        if struct.unpack_from("<I", data, base)[0] == PT_LOAD:
            aligns.append(struct.unpack_from(align_fmt, data, base + align_off)[0])
    return min(aligns) if aligns else 0


def main(path: str) -> int:
    failures = 0
    with open(path, "rb") as f, zipfile.ZipFile(f) as z:
        for info in z.infolist():
            if not info.filename.endswith(".so") or not info.filename.startswith(CHECKED_ABIS):
                continue
            problems = []
            align = min_load_alignment(z.read(info))
            if align < PAGE:
                problems.append(f"ELF LOAD align {align} < {PAGE}")
            if info.compress_type != 0:
                problems.append("compressed in zip (must be stored)")
            else:
                f.seek(info.header_offset)
                header = f.read(30)
                name_len, extra_len = struct.unpack_from("<HH", header, 26)
                data_offset = info.header_offset + 30 + name_len + extra_len
                if data_offset % PAGE:
                    problems.append(f"zip data offset {data_offset} not 16 KB aligned")
            status = "FAIL " + "; ".join(problems) if problems else "ok"
            failures += bool(problems)
            print(f"{info.filename}: {status}")
    print("RESULT:", "FAIL" if failures else "PASS")
    return 1 if failures else 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1]))
