# Wave Music — Android project

## Реализовано в проекте
- Импорт локальных аудиофайлов через системный выбор документов.
- Воспроизведение локального файла через foreground service.
- Переключатель светлой и тёмной темы.
- Вкладки «Главная», «Поиск», «Медиатека», «Настройки».
- Экран SoundCloud: поиск через собственный backend и встроенный официальный SoundCloud Widget для публичных ссылок/результатов.
- Backend-пример, который хранит SoundCloud Client ID/Secret на сервере, получает OAuth client-credentials token и проксирует поиск.

## Подключение SoundCloud поиска
SoundCloud API требует зарегистрированное приложение и OAuth token. См. официальное руководство:
https://developers.soundcloud.com/docs

1. Получите разрешённые `client_id` и `client_secret` для своего приложения SoundCloud.
2. Разверните `soundcloud-backend` на сервере с Node.js 18+.
3. Установите зависимости: `npm install`.
4. Задайте серверные переменные окружения `SOUNDCLOUD_CLIENT_ID` и `SOUNDCLOUD_CLIENT_SECRET`, затем запустите `npm start`.
5. Укажите URL backend в `gradle.properties` в корне Android-проекта:
   `SOUNDCLOUD_BACKEND_URL=https://ваш-домен`
6. Пересоберите приложение.

**Не встраивайте `client_secret` в APK.** APK можно распаковать, поэтому секрет должен оставаться только на сервере. SoundCloud может ограничивать API-доступ или отдельные треки; приложение показывает только результаты, которые API помечает как playable. Виджет использует официальный плеер SoundCloud и может иметь собственные ограничения по фоновому воспроизведению.

## Сборка APK
1. Откройте папку проекта в Android Studio.
2. Используйте JDK 17 и Android SDK 35.
3. Дождитесь Gradle Sync.
4. Build → Build Bundle(s) / APK(s) → Build APK(s).
5. APK будет в `app/build/outputs/apk/debug/app-debug.apk`.

Package: `com.example.wavemusic`
Min SDK: 26
Target SDK: 35

## Ограничения текущего прототипа
Фоновое воспроизведение локальных файлов реализовано через foreground service. Для полноценного управления с экрана блокировки (play/pause/seek, MediaSession и корректные media notification controls) потребуется следующий этап доработки. Встроенный SoundCloud Widget — официальный web-плеер; его поведение в фоне зависит от Android/WebView и ограничений SoundCloud.
