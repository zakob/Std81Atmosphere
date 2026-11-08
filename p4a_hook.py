"""p4a hooks: make the window as HWUI-inert as possible.

On Android 16 / ColorOS 16 the system added a FORTIFY check that aborts on
"pthread_mutex_lock called on a destroyed mutex" (bionic change).  This turns
a pre-existing renderer race into a SIGABRT for Kivy/SDL apps: at startup with
newer p4a, and on input focus (keyboard/insets) even with the old toolchain.
The crashing threads are the HWUI renderer threads (RenderThread, hwuiTask).

Kivy draws on its own SurfaceView/OpenGL surface, so the window itself does
not need hardware acceleration; disabling it removes the HWUI render threads
from the process.  With the keyboard shown, ``adjustNothing`` prevents any
window resize/pan, avoiding the insets code path as well.

The manifest is rendered *after* the ``before_apk_build`` hook, so the actual
patch happens in ``after_apk_build`` (right before the gradle assemble).  The
hook logs every action so a build log shows whether the manifest was patched.
See https://github.com/kivy/python-for-android#hooks for the hook protocol.
"""

import os


def _patch_file(path):
    with open(path, encoding='utf-8') as stream:
        text = stream.read()
    original = text

    # Force hardware acceleration off (replace, or add when absent).
    if 'android:hardwareAccelerated="true"' in text:
        text = text.replace(
            'android:hardwareAccelerated="true"',
            'android:hardwareAccelerated="false"',
        )
        reinforced = True
    elif 'android:hardwareAccelerated="false"' in text:
        reinforced = True
    else:
        text = text.replace('<application ', '<application android:hardwareAccelerated="false" ')
        reinforced = 'hardwareAccelerated added'

    # Do not resize the window when the soft keyboard is shown.
    if 'android:windowSoftInputMode' not in text:
        text = text.replace(
            '<activity ',
            '<activity android:windowSoftInputMode="adjustNothing" ',
        )
        softinput = 'added'
    else:
        softinput = 'already set'

    if text != original:
        with open(path, 'w', encoding='utf-8') as stream:
            stream.write(text)
        print('[p4a hook] patched {}: hwaccel={}, softinputMode={}'.format(
            path, reinforced, softinput))
        return 1
    print('[p4a hook] {}: no changes needed (hwaccel={}, softinput={})'.format(
        path, reinforced, softinput))
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

    manifests = _find_manifests(dist_dir)
    print('[p4a hook] manifests found: {}'.format(manifests))
    patched = 0
    for path in manifests:
        patched += _patch_file(path)
    if patched == 0:
        message = '[p4a hook] WARNING: no manifest patched ({} manifests checked)'
        print(message.format(len(manifests)))
