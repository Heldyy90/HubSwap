<div align="center">

# ⚡ HubSwap

### Fabric-мод для быстрого перехода между серверами HolyWorld

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-5E7C16?style=for-the-badge)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-1.21.11-DBD0B4?style=for-the-badge)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-E76F00?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

[![Скачать HubSwap](https://img.shields.io/badge/📥_Скачать_HubSwap-Releases-2ea44f?style=for-the-badge)](https://github.com/Heldyy90/HubSwap/releases)

</div>

---

## 🧩 Информация о моде

| Параметр | Значение |
|---|---|
| 🎮 Minecraft | **1.21.11** |
| 🧵 Mod Loader | **Fabric** |
| ☕ Java | **21** |
| 📦 Версия HubSwap | **1.0.8** |
| 📄 Лицензия | **MIT** |

> Для Minecraft **1.20.1** используется отдельная ветка [`mc-1.20.1`](https://github.com/Heldyy90/HubSwap/tree/mc-1.20.1). JAR от 1.21.11 не подходит для Minecraft 1.20.1.

---

## 📌 Описание

**HubSwap** — Fabric-мод для игроков и модераторов **HolyWorld**, который ускоряет переход между серверами и избавляет от ручного поиска нужной анархии в меню.

Поддерживаются режимы **Classic**, **Lite**, **Lite 1.20** и **Prime**, быстрые команды, хоткеи, кликабельные названия серверов в чате, статистика переходов, автоматическая настройка задержек и настраиваемые диапазоны Lite-анархий.

---

## 📊 Статистика проекта

| Показатель | Значение |
|---|---:|
| 📥 Всего скачиваний файлов | **643** |
| 🧩 Скачиваний `.jar` модов | **614** |
| 🚀 Всего релизов | **9** |
| 📦 Всего файлов в релизах | **13** |
| 🆕 Последний опубликованный релиз | **[`v.1.0.8`](https://github.com/Heldyy90/HubSwap/releases/tag/v.1.0.8)** |
| 📅 Дата последнего релиза | **2026-08-19** |

[➡️ Все релизы HubSwap](https://github.com/Heldyy90/HubSwap/releases)

---

## 📸 Скриншоты

### Главное окно мода

<img width="655" height="342" alt="image" src="https://github.com/user-attachments/assets/b7e7500c-7a15-4c33-b4f5-bf5e19a240fa" />

### Настройки, задержки и команды

<img width="1280" height="1024" alt="image" src="https://github.com/user-attachments/assets/c1d5f984-389e-4ace-a44d-2c73c32c6ad9" />

---

## ✨ Возможности

| Возможность | Описание |
|---|---|
| ⚡ Быстрый переход | Автоматический переход через хаб, выбор режима и нужного сервера |
| 🏹 Classic | Быстрый переход на Classic-анархии через `/cn` |
| 💡 Lite | Переход на Lite через `/ln` |
| 🧱 Lite 1.20 | Переход через `/ln120` |
| ⭐ Prime | Переход через `/pn` |
| ⇆ Диапазоны Lite | Отдельные диапазоны Соло / Дуо / Трио / Клан |
| 🌐 Общие диапазоны | `anarchy_ranges.json` проверяется клиентом примерно раз в 15 минут |
| 🖱️ Кликабельный чат | `prime`, `prime3`, `Prime-5` и другие названия серверов можно нажимать прямо в чате |
| 🌐 RU / EN раскладка | Команды работают и на английской, и на русской раскладке клавиатуры |
| ⌨️ Хоткеи | Быстрый запуск переходов с назначенных клавиш |
| 📈 Статистика | Статистика переходов и времени на серверах |
| ⏱️ Автонастройка | Автоматический подбор задержек для более стабильного перехода |
| 🎨 Настройки | Темы, цвета, задержки, команды и другие параметры интерфейса |
| 🔔 Обновления | Удалённые уведомления через `announcement.json` |

---

## ⇆ Диапазоны Lite-анархий

Диапазоны задаются в [`anarchy_ranges.json`](anarchy_ranges.json). Клиенты с включённой настройкой **«Общие диапазоны»** проверяют файл примерно раз в 15 минут и автоматически применяют изменения.

Подробная инструкция: [`ANARCHY_RANGES.md`](ANARCHY_RANGES.md).

---

## ⌨️ Основные команды

| Команда | Назначение |
|---|---|
| `/cn <номер>` | Classic |
| `/ln <номер>` | Lite |
| `/ln120 <номер>` | Lite 1.20 |
| `/pn <номер>` | Prime |

Открыть настройки HubSwap:

```text
F6
```

---

## 🧱 Версии Minecraft

| Ветка | Minecraft | Java | Назначение |
|---|---|---|---|
| [`main`](https://github.com/Heldyy90/HubSwap/tree/main) | **1.21.11** | **21** | Актуальная версия |
| [`mc-1.20.1`](https://github.com/Heldyy90/HubSwap/tree/mc-1.20.1) | **1.20.1** | **17** | Версия для Minecraft 1.20.1 |

Версии не смешиваются: каждая версия Minecraft хранится в своей ветке, а версия Minecraft указывается прямо в имени JAR.

```text
HubSwap-1.21.11-1.0.8.jar
HubSwap-1.20.1-1.0.8.jar
```

---

## 🔨 Сборка

Для сборки версии **1.21.11** нужна **Java 21**.

```powershell
java -version
./gradlew clean build
```

Готовый JAR появится в:

```text
build/libs/HubSwap-1.21.11-1.0.8.jar
```

---

## 📞 Контакты

| Платформа | Контакт |
|---|---|
| Discord | `heldyy` |
| VK | [vk.com/id771537594](https://vk.com/id771537594) |

---

## 📄 Лицензия

Проект распространяется по лицензии **MIT**.
