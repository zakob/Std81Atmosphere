# Std81Atmosphere

App for calculating atmosphere parameters on height accoding **GOST 4401-81**.

---

## Build for Android

1. Create image with **buildozer** according to https://github.com/kivy/buildozer?tab=readme-ov-file#buildozer-docker-image

2. Run container with **buildozer**:
```
docker run --interactive --tty --rm --volume "$HOME/.buildozer":/home/user/.buildozer --volume "$PWD":/home/user/hostcwd --entrypoint /bin/bash kivy/buildozer
```
3. Set key if already exists:
```
export P4A_RELEASE_KEYSTORE=...
export P4A_RELEASE_KEYSTORE_PASSWD=...
export P4A_RELEASE_KEYALIAS_PASSWD=...
export P4A_RELEASE_KEYALIAS=...
```
If it doesn't exist, create it with `keytool` (https://gist.github.com/Guhan-SenSam/fa4ed215ef3419e7b3154de5cb71f641)

4. Run build:
```
buildozer android release
```

---

Note:
 - You don't need a key for the debugging build.
 - In WSL you may need to fix the folder `$HOME/.buildozer` rights.
