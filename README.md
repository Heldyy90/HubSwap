<div align="center">

# ⚡ HubSwap — Minecraft 1.21.11

Fabric-мод для быстрого перехода между серверами HolyWorld.

</div>

## Эта ветка

Это исходный код **HubSwap для Minecraft 1.21.11 Fabric**.

- Minecraft: **1.21.11**
- Fabric Loader: **0.19.3+**
- Fabric API: **0.141.6+1.21.11**
- Java: **21**
- Версия HubSwap: **1.0.7**

> Не используй JAR этой ветки на Minecraft 1.20.1.

## Возможности

- Classic, Lite, Lite 1.20 и Prime.
- `/cn`, `/ln`, `/ln120`, `/pn`.
- Prime 1–9.
- Кликабельные названия серверов в чате, включая `prime`, `prime3`, `Prime-5`.
- Поддержка команд на русской раскладке.
- Хоткеи, статистика, автонастройка задержек.
- Удалённые объявления через `announcement.json` без перезапуска Minecraft.
- Проверка обновлений и кнопки «Скачать» / «Скрыть уведомление».

## Сборка

Нужна Java 21.

```powershell
java -version
./gradlew clean build
```

Готовый JAR появится в:

```text
build/libs/HubSwap-1.21.11-1.0.7.jar
```

## Разделение версий

Рекомендуемая структура репозитория:

- `main` — актуальная версия, сейчас **Minecraft 1.21.11**.
- `mc-1.20.1` — поддерживаемая ветка для Minecraft 1.20.1.
- теги релизов: `v1.0.7-mc1.21.11`, `v1.0.7-mc1.20.1`.
- JAR: `HubSwap-1.21.11-1.0.7.jar`, `HubSwap-1.20.1-1.0.7.jar`.

Подробнее: [`VERSIONING.md`](VERSIONING.md).

## Удалённые объявления

Настройка описана в [`REMOTE_NOTICE.md`](REMOTE_NOTICE.md).

## Лицензия

MIT.
