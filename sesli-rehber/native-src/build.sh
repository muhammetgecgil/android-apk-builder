#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd); cd "$ROOT"
TARGET=aarch64-linux-android30
mkdir -p out apk/lib/arm64-v8a apk/res/drawable
clang --target=$TARGET -fPIC -shared -nostdlib -fuse-ld=lld -Wl,-soname,libandroid.so stubs/android.c -o stubs/libandroid.so
clang --target=$TARGET -fPIC -shared -nostdlib -fuse-ld=lld -Wl,-soname,libcamera2ndk.so stubs/camera2ndk.c -o stubs/libcamera2ndk.so
clang --target=$TARGET -fPIC -shared -nostdlib -fuse-ld=lld -Wl,-soname,libmediandk.so stubs/mediandk.c -o stubs/libmediandk.so
clang --target=$TARGET -fPIC -shared -nostdlib -fuse-ld=lld -Wl,-soname,libc.so stubs/libc.c -o stubs/libc.so
clang --target=$TARGET -fPIC -shared -nostdlib -fuse-ld=lld -Wl,-soname,libm.so stubs/libm.c -o stubs/libm.so
clang --target=$TARGET -march=armv8-a -mno-outline-atomics -fPIC -O2 -fvisibility=hidden -fno-stack-protector -fno-builtin -Iinclude -c src/main.c -o out/main.o
clang --target=$TARGET -shared -nostdlib -fuse-ld=lld -Wl,-soname,libskytrack.so -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,-z,now -Wl,-z,relro -L stubs out/main.o -landroid -lcamera2ndk -lmediandk -lc -lm -o apk/lib/arm64-v8a/libskytrack.so
python3 build_manifest.py
echo "Native library and manifest built. Package/sign with your Android signing key."
