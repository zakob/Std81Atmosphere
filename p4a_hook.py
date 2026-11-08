"""p4a hooks: disable the window hardware acceleration flag.

On some ColorOS 16 / Android 16 devices the system HWUI renderer crashes when
Kivy/SDL applications start (SIGSEGV in the RenderThread and "pthread_mutex_lock
called on a destroyed mutex" in the hwuiTask threads).  Kivy draws on its own
SurfaceView/OpenGL surface, so the window itself does not need hardware
acceleration; disabling it avoids that HWUI code path.

The manifest is rendered *after* the ``before_apk_build`` hook, so the actual
patch happens in ``after_apk_build`` (right before the gradle assemble).  The
hook logs every action so a build log shows whether the manifest was patched.
See https://github.com/kivy/python-for-android#hooks for the hook protocol.
"""

import glob
import os


def _patch_file(path):
    with open(path, encoding='utf-8') as stream:
        text = stream.read()
    updated = text.replace(
        'android:hardwareAccelerated="true"',
        'android:hardwareAccelerated="false"',
    )
    if updated != text:
        with open(path, 'w', encoding='utf-8') as stream:
            stream.write(updated)
        print('[p4a hook] patched {}: hardwareAccelerated true -> false'.format(path))
        return 1
    print('[p4a hook] {}: hardwareAccelerated already false or absent'.format(path))
    return 0


def _find_manifests(dist_dir):
    patterns = (
        os.path.join(dist_dir, 'src', 'main', 'AndroidManifest.xml'),
        os.path.join(dist_dir, 'AndroidManifest.xml'),
    )
    found = [path for path in patterns if path and os.path.exists(path)]
    for root, _dirs, _files in os.walk(dist_dir):
        if 'AndroidManifest.xml' in os.listdir(root):
            found.append(os.path.join(root, 'AndroidManifest.xml'))
    return sorted(set(found))


def after_apk_build(toolchain):
    """Patch the rendered AndroidManifest.xml before the gradle assemble."""
    dist = getattr(toolchain, '_dist', None)
    dist_dir = getattr(dist, 'dist_dir', None)
    if not dist_dir or not os.path.isdir(dist_dir):
        print('[p4a hook] WARNING: no dist_dir at {}'.format(dist_dir))
        return

    print('[p4a hook] dist_dir contents: {}'.format(sorted(os.listdir(dist_dir))[:20]))
    manifests = _find_manifests(dist_dir)
    print('[p4a hook] manifests found: {}'.format(manifests))
    patched = 0
    for path in manifests:
        patched += _patch_file(path)
    if patched == 0:
        print('[p4a hook] WARNING: no manifest patched ({} manifests checked)'.format(len(manifests)))