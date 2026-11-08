"""p4a hook: disable the window hardware acceleration flag.

On ColorOS 16 (Android 16) the system HWUI renderer crashes at app startup for
Kivy/SDL applications (SIGSEGV in the RenderThread and "pthread_mutex_lock
called on a destroyed mutex" in the hwuiTask threads).  Kivy draws on its own
SurfaceView/OpenGL surface, so the window itself does not need hardware
acceleration; disabling it avoids the crashing HWUI code path.

See https://github.com/kivy/python-for-android#hooks for the hook protocol.
"""

import glob
import os


def before_apk_build(toolchain):
    """Patch the generated AndroidManifest.xml right before the APK is built."""
    dist = getattr(toolchain, '_dist', None)
    dist_dir = getattr(dist, 'dist_dir', None)
    storage_dir = getattr(getattr(toolchain, 'ctx', None), 'storage_dir', None)

    candidates = []
    if dist_dir:
        candidates.append(os.path.join(dist_dir, 'src', 'main', 'AndroidManifest.xml'))
        candidates.append(os.path.join(dist_dir, 'AndroidManifest.xml'))
    if storage_dir:
        candidates.extend(glob.glob(os.path.join(storage_dir, 'dists', '*',
                                                 'src', 'main', 'AndroidManifest.xml')))
        candidates.extend(glob.glob(os.path.join(storage_dir, 'dists', '*',
                                                 'AndroidManifest.xml')))

    patched = 0
    for path in candidates:
        if not path or not os.path.exists(path):
            continue
        with open(path, encoding='utf-8') as stream:
            text = stream.read()
        updated = text.replace(
            'android:hardwareAccelerated="true"',
            'android:hardwareAccelerated="false"',
        )
        if updated != text:
            with open(path, 'w', encoding='utf-8') as stream:
                stream.write(updated)
            patched += 1
            print('[p4a hook] patched {}: hardwareAccelerated true -> false'.format(path))
    if patched == 0:
        print('[p4a hook] WARNING: no AndroidManifest.xml found to patch')