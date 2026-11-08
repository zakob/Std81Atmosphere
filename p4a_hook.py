"""p4a hook: keep the window resize-inert when the soft keyboard appears.

On Android 16 / ColorOS 16 a FORTIFY check in bionic aborts on
"pthread_mutex_lock called on a destroyed mutex", exposing a pre-existing
renderer race in this ROM.  For this app the crash was observed on input focus
when the keyboard triggers a window insets change.

Earlier experiments showed that disabling ``hardwareAccelerated`` made the
focus crash *worse* (a software-rendered window plus IME is a known-bad
combination on some ROMs), so this hook only adds
``android:windowSoftInputMode='adjustNothing'``: with it the window neither
resizes nor pans when the keyboard shows, so the insets path is not taken at
all.  The height field sits at the top of the screen, so a covering keyboard
is not a problem.

The manifest is rendered *after* the ``before_apk_build`` hook, so the patch
happens in ``after_apk_build``.  See
https://github.com/kivy/python-for-android#hooks for the hook protocol.
"""

import os


def _patch_file(path):
    with open(path, encoding='utf-8') as stream:
        text = stream.read()
    original = text

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
        print('[p4a hook] patched {}: softinputMode={}'.format(path, softinput))
        return 1
    print('[p4a hook] {}: no changes needed (softinput={})'.format(path, softinput))
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
