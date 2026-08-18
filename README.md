README
Achreon Fabric Mod (Minecraft 1.21.11)
Исходный код клиентского мода Achreon на базе Fabric Mod Loader для Minecraft 1.21.11.
 Требования для сборки
Java Development Kit (JDK): Java 21 (например, Eclipse Temurin 21 или Oracle/Amazon Corretto JDK 21).
Система сборки: Gradle Wrapper уже включен в проект (gradlew / gradlew.bat).
Интернет-соединение: Для первой загрузки зависимостей Fabric Loom, Yarn маппингов и библиотек Minecraft.
 Инструкция по сборке
Windows (через командную строку / PowerShell):
gradlew.bat build
​
или в PowerShell:
.\gradlew.bat build
​
Linux / macOS (в терминале):
chmod +x gradlew
./gradlew build
​
 Результат сборки
После успешной сборки готовый .jar файл мода будет находиться в папке:
build/libs/Atheryx-1.0-SNAPSHOT.jar
 Открытие в среде разработки (IDE)
Откройте IntelliJ IDEA (рекомендуется Community или Ultimate).
Выберите File -> Open... и укажите папку с данным проектом.
Дождитесь завершения синхронизации Gradle.
Для запуска клиента прямо из среды разработки выполните Gradle таску:
Tasks -> fabric -> runClient (или .\gradlew.bat runClient).
