# HubSwap (Fabric 1.20.1)

Мод для модераторов на **HolyWorld**, который ускоряет и упрощает переход между серверами **Anarchy / Lite-Anarchy**:
- быстрые команды вида /cn 3, /ln 27, /ln120 2
- кликабельные названия серверов в чате (например lanarchy30, Lite-Anarchy-40, Anarchy-8)
- меню настроек (клавиша **F7** по умолчанию)
- уведомления о переходе и настройка визуальной темы



## ✨ Возможности

 **Автопереход** на нужный сервер через последовательность действий (хаб → выбор → переход)
 **Задержки**
  - задержка после /hub (для прогрузки меню)
  - задержка между кликами
 **Настраиваемые команды**
  - Classic: cn (по умолчанию)
  - Lite: ln (по умолчанию)
  - Lite 1.20: ln120 (по умолчанию)
 **RU/EN раскладка**
  - команды регистрируются также под русской раскладкой той же клавиатуры (QWERTY → ЙЦУКЕН)
 **Кликабельные ссылки в чате**
  - распознаёт: Anarchy`, Anarchy-8, anarchy8, Lite-Anarchy-40, lanarchy30 и т.п.
  - нажатие отправляет команду перехода
 **Настройки интерфейса**
  - переключаемая цветовая тема элементов
  - отдельный цвет кликабельных ссылок
 **Уведомления** о переходе (вкл/выкл)


## ⌨️ Управление

 Открыть настройки: **F7**
- Примеры команд:
  - /cn 1 … /cn 8 — Classic Anarchy
  - /ln 1 … /ln 69 — Lite-Anarchy
  - /ln120 1 … /ln120 3 — Lite 1.20

---

## 📦 Установка

1. Установи **Fabric Loader** и **Fabric API** под свою версию Minecraft.
2. Скачай HubSwap-*.jar из **Releases**.
3. Положи файл в папку:
   - .minecraft\mods
   - .minecraft\labymod-neo\fabric\1.20.1\mods (Если играете с LabyMod и используете дополнение **Fabric Loader**
4. Запусти игру.


## 🧱 Сборка из исходников

Требования:
- **Java 17**
- Gradle (можно через ./gradlew)

Команды:
`bash
./gradlew build

## 📸 Скриншоты
**Меню настроек: задержки и команды**

<img width="1280" height="1024" alt="image" src="https://github.com/user-attachments/assets/2e65dce9-043a-4bed-98ec-79a73afc0d08" />

**Кликабельные сервера в чате**

<img width="313" height="203" alt="image" src="https://github.com/user-attachments/assets/f189b0fc-d50e-4c4a-8e44-b730bf26b155" />

<img width="313" height="198" alt="image" src="https://github.com/user-attachments/assets/be45dc74-2318-4f7e-8a6c-4223f49e0a6c" />

<img width="460" height="85" alt="image" src="https://github.com/user-attachments/assets/e8d3b98d-5545-46b8-afad-1ce3ce9df913" />


