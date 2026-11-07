# Сборка и проверки на GitHub Actions

В репозитории два workflow-файла:

| Файл | Когда запускается | Что делает | Время |
|---|---|---|---|
| `.github/workflows/ci.yml` | каждый push в любую ветку и каждый pull request | `ruff check .` и `pytest -q` | ~1 минута |
| `.github/workflows/android.yml` | вручную (кнопка **Run workflow**) | debug APK, выкладывается артефактом | 10–60 минут |
| `.github/workflows/android.yml` | push тега `v*` | подписанный release APK + GitHub Release | 10–60 минут |

---

## 1. Где смотреть результаты

1. Откройте репозиторий на GitHub.
2. Вкладка **Actions**.
3. Слева выберите нужный workflow (`CI` или `Android`).
4. Нажмите на конкретный запуск — там логи по шагам и артефакты.

Зелёная галочка — успех, красный крестик — ошибка (откройте шаг с ошибкой и
посмотрите лог).

---

## 2. Проверки на каждый push (CI)

Ничего настраивать не нужно: после push в ветку GitHub сам запустит `CI`
и покажет результат во вкладке Actions.

Локально те же проверки можно повторить:

```sh
python -m pip install -r requirements-dev.txt
ruff check .
pytest -q
```

Если у вас нет Python — тесты также запускаются стандартным `unittest`:

```sh
python -m unittest discover -s tests -v
```

---

## 3. Ручная сборка debug APK

Нужен, когда хочется проверить приложение на телефоне без релиза.

1. Workflow `Android` должен быть в ветке **main** (GitHub показывает кнопку
   **Run workflow** только для workflow, которые есть в ветке по умолчанию).
   Поэтому сначала влейте ветку с этими файлами в `main`.
2. GitHub → **Actions** → **Android** → кнопка **Run workflow** (справа).
3. Выберите ветку (обычно `main`) → **Run workflow**.
4. Дождитесь окончания (первый запуск самый долгий: скачиваются Android
   SDK/NDK).
5. Внизу страницы запуска появится блок **Artifacts** → `std81-debug-apk`.
   Скачайте zip и распакуйте — внутри `.apk`.

Debug APK подписан отладочным ключом, для личного использования этого
достаточно.

---

## 4. Release APK по тегу

1. Убедитесь, что CI на нужном коммите зелёный.
2. При необходимости поднимите версию в `buildozer.spec` (`version = 0.9`).
3. Создайте и отправьте тег:

   ```sh
   git tag v0.9.0
   git push origin v0.9.0
   ```

4. GitHub запустит workflow `Android`, job **Release APK**:
   - соберёт APK, подписанный вашим ключом;
   - создаст **GitHub Release** с тегом `v0.9.0`;
   - прикрепит APK к релизу.
5. Готовый файл можно скачать на вкладке **Releases**.

Release собирается только для тегов, начинающихся с `v` (`v0.9.0`,
`v1.0.0`, …).

---

## 5. Секреты для подписи

Подпись release-сборки настраивается один раз:

**Settings → Secrets and variables → Actions → New repository secret**

| Имя | Значение |
|---|---|
| `KEYSTORE_BASE64` | содержимое keystore-файла в base64 (одна строка) |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | псевдоним ключа (в текущем keystore — `std81`) |
| `KEY_ALIAS_PASSWORD` | пароль ключа |

Получить base64 (PowerShell):

```powershell
[Convert]::ToBase64String(
    [IO.File]::ReadAllBytes("C:\Users\Mi\projects\Std81Atmosphere\keystores\std81atmosphere.keystore")
) | Set-Clipboard
```

Linux/macOS:

```sh
base64 -w0 keystores/std81atmosphere.keystore
```

Вставьте полученную строку целиком в секрет `KEYSTORE_BASE64`.

> **Важно.** Keystore и пароли не должны попадать в git — каталог `keystores/`
> уже в `.gitignore`. Если файл или пароль когда-либо утекал, выпустите новый
> ключ: приложение, подписанное другим ключом, нельзя обновить поверх старого.

---

## 6. Как это устроено

* `ci.yml` — `actions/checkout`, `actions/setup-python` (Python 3.11, как в
  Android-сборке), установка `requirements-dev.txt`, затем `ruff` и `pytest`.
* `android.yml` — запускает официальный образ `ghcr.io/kivy/buildozer:latest`
  через `docker run`: проект монтируется в `/home/user/hostcwd`, внутри
  выполняется `buildozer android debug` или `buildozer android release`.
  Официальный `entrypoint.sh` сам подставляет UID/GID хозяина каталога и
  сохраняет переменные окружения (поэтому `P4A_RELEASE_*` доходят до p4a).
* Шаг **Free disk space** удаляет предустановленный Android SDK и .NET с
  раннера: они не нужны (buildozer качает свой SDK/NDK в `~/.buildozer`) и
  занимают около 11 ГБ.
* Кэш `~/.buildozer` (Android SDK/NDK и зависимости) ускоряет повторные сборки.
* Подпись release-сборки — через переменные `P4A_RELEASE_KEYSTORE`,
  `P4A_RELEASE_KEYSTORE_PASSWD`, `P4A_RELEASE_KEYALIAS`,
  `P4A_RELEASE_KEYALIAS_PASSWD`; keystore декодируется из секрета в
  `keystores/release.keystore` и виден в контейнере как
  `/home/user/hostcwd/keystores/release.keystore`.

---

## 7. Ограничения и время

* Публичный репозиторий — Actions бесплатны; приватный — 2000 минут в месяц
  (сборка APK съедает больше всего).
* Первая сборка APK: ~30–60 минут (скачивание SDK/NDK). С кэшем — обычно
  10–20 минут.
* Debug-сборка запускается вручную, чтобы не тратить минуты на каждый push.

---

## 8. Если что-то пошло не так

| Симптом | Что проверить |
|---|---|
| Нет кнопки **Run workflow** | workflow ещё не в ветке `main` (или Actions отключены в Settings → Actions → General) |
| Job упал на `pip install` | доступность PyPI, синтаксис `requirements-dev.txt` |
| `ruff` нашёл ошибки | запустите `ruff check .` локально и исправьте |
| Нет APK в артефактах | лог шага **Build**; часто это ошибка в `buildozer.spec` или нехватка места |
| `docker: Error response ... pull access denied` | образ `ghcr.io/kivy/buildozer:latest` недоступен; перезапустите job или проверьте доступ к ghcr.io |
| Ошибка подписи | секреты `KEYSTORE_*`; в контейнере keystore должен быть по пути `/home/user/hostcwd/keystores/release.keystore` |
| `No space left on device` | шаг **Free disk space** освобождает ~11 ГБ; при повторе можно удалить ещё `/opt/hostedtoolcache` |
| Ошибки компиляции Kivy (`_PyInterpreterState_GetConfig`, `Py_UNICODE`) | p4a собрал слишком новый Python; в `buildozer.spec` зафиксирован `python3==3.12.8` — не убирайте пин |
| Сборка долго висит | первый запуск качает SDK/NDK; последующие должны идти быстрее |

Логи всегда доступны на странице запуска: **Actions → запуск → job → шаг**.
