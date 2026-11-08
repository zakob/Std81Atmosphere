# Сборка и проверки на GitHub Actions

Единый workflow `.github/workflows/android.yml` с тремя джобами:

| Джоба | Когда | Что делает | Время |
|---|---|---|---|
| `test` | каждый push в любую ветку и каждый PR | `gradle testDebugUnitTest lintDebug` | ~2–5 мин |
| `debug-apk` | вручную (кнопка **Run workflow**) | `gradle assembleDebug`, APK артефактом | ~5–10 мин |
| `release-apk` | push тега `v*` | подписанный release-APK + GitHub Release | ~5–10 мин |

---

## 1. Проверки на каждый push

Ничего настраивать не нужно — после `git push` откройте вкладку **Actions**:
GK будет запуск `Android / test` (unit-тесты + lint). Локально то же самое:

```sh
gradle testDebugUnitTest lintDebug
```

JDK 17 обязателен (AGP 8.7/Kotlin 2.0).

## 2. Ручная сборка debug APK

Workflow должен существовать в ветке по умолчанию (`main`):

1. **Actions → Android → Run workflow** → выбрать `main` → **Run workflow**.
2. Дождаться окончания, внизу страницы — **Artifacts → std81-debug-apk** (zip, внутри `.apk`).

Debug APK подписан debug-ключом, для проверки на телефоне достаточно.

## 3. Release APK по тегу

1. Убедитесь, что `test` зелёный на `main`.
2. При необходимости поднимите `versionName`/`versionCode` в
   `app/build.gradle.kts` (версии должны расти между релизами: 1.0.0 → 1.0.1).
3. Создайте и отправьте тег:

   ```sh
   git tag -a v1.0.0 -m "Std81Atmosphere 1.0.0"
   git push origin v1.0.0
   ```

4. `release-apk` соберёт **подписанный** APK и создаст GitHub Release с файлом.

## 4. Секреты для подписи

**Settings → Secrets and variables → Actions → New repository secret**:

| Имя | Значение |
|---|---|
| `KEYSTORE_BASE64` | содержимое keystore-файла в base64 (одна строка) |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | псевдоним ключа (текущий — `std81`) |
| `KEY_ALIAS_PASSWORD` | пароль ключа |

Base64 (PowerShell):

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("путь\к\keystore.jks")
) | Set-Clipboard
```

Workflow декодирует keystore в `keystores/release.keystore` и передаёт путь и
пароли через переменные окружения `RELEASE_KEYSTORE_*` (их читает
`app/build.gradle.kts` из `System.getenv`).

**Важно:** keystore и пароли не должны попадать в git (`keystores/` в
`.gitignore`). Потеря ключа = невозможность обновить установленное
приложение.

## 5. Права GitHub Actions

**Settings → Actions → General → Workflow permissions → Read and write
permissions** — иначе шаг публикации Release может упасть с 403.

## 6. Если что-то пошло не так

| Симптом | Что проверить |
|---|---|
| Нет кнопки **Run workflow** | workflow ещё не в `main` или Actions отключены |
| `compileSdk 35 ... unsupported` | AGP < 8.7; обновите `agp` в `gradle/libs.versions.toml` |
| Ошибка подписи | имена секретов `KEYSTORE_*`, alias, base64 одной строкой |
| `Release` шаг → 403 | Workflow permissions → Read and write |
| Нет APK в артефактах | лог шага Build; путь `app/build/outputs/apk/...` |
| JDK-ошибки | нужен JDK 17 |

Логи: **Actions → запуск → джоба → шаг**.