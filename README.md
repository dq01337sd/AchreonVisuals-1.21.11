# Atheryx Fabric Mod (Minecraft 1.21.11)

Исходный код клиентского мода Atheryx на базе Fabric Mod Loader для Minecraft 1.21.11.

---

## 🛠 Требования для сборки
* **Java Development Kit (JDK)**: Java 21 (например, [Eclipse Temurin 21](https://adoptium.net/) или Oracle/Amazon Corretto JDK 21).
* **Система сборки**: Gradle Wrapper уже включен в проект (`gradlew` / `gradlew.bat`).
* **Интернет-соединение**: Для первой загрузки зависимостей Fabric Loom, Yarn маппингов и библиотек Minecraft.

---

## 🚀 Инструкция по сборке

### Windows (через командную строку / PowerShell):
```cmd
gradlew.bat build
```
или в PowerShell:
```powershell
.\gradlew.bat build
```

### Linux / macOS (в терминале):
```bash
chmod +x gradlew
./gradlew build
```

---

## 📦 Результат сборки
После успешной сборки готовый `.jar` файл мода будет находиться в папке:
`build/libs/Atheryx-1.0-SNAPSHOT.jar`

---

## 💻 Открытие в среде разработки (IDE)
1. Откройте **IntelliJ IDEA** (рекомендуется Community или Ultimate).
2. Выберите **File -> Open...** и укажите папку с данным проектом.
3. Дождитесь завершения синхронизации Gradle.
4. Для запуска клиента прямо из среды разработки выполните Gradle таску:
   `Tasks -> fabric -> runClient` (или `.\gradlew.bat runClient`).
