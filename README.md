# Автономное копирование истории браузера
Эта программа нужна для считывания истории пользователя из Google Chrome или Firefox, считывание происходит 1 раз в 30 секунд.

# Как работает программа

Программа запускается после того, как пользователь входит на рабочий стол. (ярлык bat файла положен в глобальную автозагрузку )
В указанной, как аргумент, папке, происходит создание папки ResultHistory, в которую складываются файлы истории каждого из пользователей по дням.
Раз в 30 секунд происходит считывание полной истории указанного браузера за весь текущий день, начиная с 00:00:00 и из этого сета
вычитаем те записи, что находятся в текстовом файле, таким образом мы добавим только те строки, которых еще нет.
# Установка и запуск

## Установка JAVA (change runtime environment, on Windows 8 Server for example)
Важно! Перед запуском программ необходимо убедиться, что на компьютере установлена JAVA 8 (jre 8). 

Как загрузить и установить java 8. Смена java runtime environment.
1) Скачать файл установки с официального сайта, версия jre-8u261-windows-i586: https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html#license-lightbox 
2) Нажать правой кнопкой мыши на скачанный файл (Файл находится в папке “Загрузка”), выбрать «Запуск от имени администратора», появится окно установки, нажать на checkbox (соглашение с правилами Java), нажать “Next”. После установки закрыть окно установщика. 
3) Убедиться, что по пути C:\Program Files\Java появилась папка jre1.8.0_261 
4) Зайти в Панель управления-> Система-> Дополнительные параметры системы-> Переменные среды найти в «Системные переменные» переменную Path, один раз нажать на нее, нажать кнопку ниже «Изменить»
![img_1.png](screenshots/img_1.png)
   Стереть “C:\Program Files\Common Files\Oracle\Java\javapath;”:
![img_2.png](screenshots/img_2.png)
5) В проводнике по пути C:\ProgramData\Oracle\Java создать папку javapathjre1.8.0_261, удалить папку javapath(если такая папка будет существовать) 
6) Открыть командную строку с правами администратора («Запуск от имени администратора»), скопировать следующий путь: C:\ProgramData\Oracle\Java\javapathjre1.8.0_261
   Исполнить команды, как на скриншоте:
![img_3.png](screenshots/img_6.png)
Строки для исполнения:
```cmd
cd C:\ProgramData\Oracle\Java\javapathjre1.8.0_261 	
mklink java.exe "C:\Program Files\Java\jre1.8.0_261\bin\java.exe" 	
mklink javaw.exe "C:\Program Files\Java\jre1.8.0_261\bin\javaw.exe" 	
mklink javaws.exe "C:\Program Files\Java\jre1.8.0_261\bin\javaws.exe"
```

8) Подняться на один уровень вверх и создать символьную ссылку на javapath с помощью следующих команд: 	
```cmd
cd ..
mklink /D javapath javapathjre1.8.0_261
```

9) Закрыть командную строку. Открыть пуск, в поиске найти «Выполнить», написать внутри поиска «regedit», нажать “enter”, откроется новое окно, в этом окне найти слева папку 
   HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment\CurrentVersion и убедиться, что CurrentVersion имеет значение 1.8, закрыть. 
10) Скачать драйвер по ссылке https://www.microsoft.com/ru-ru/download/details.aspx?id=48145.
    Установить скачанный драйвер С++ vc_redist.x86 (правая кнопка мыши -> "Установить как администратор")
11) Проверить, что Java установлена, открыть командную строку, ввести команду «java -version»:
![img_4.png](screenshots/img_3.png)
12) Открыть командную строку от имени администратора(как в пункте 2), в командной строке ввести  
```cmd
cd C:\Program Files\Java\ 
mklink /D CurrentVersion .\jre1.8.0_261
```

13) зайти в панель управления-> система->  дополнительные параметры систмы->переменные среды, нажать в Системных меременных «создать», в открывшемся окне: имя - JAVA_HOME, 
    значение - C:\Program Files\Java\CurrentVersion
![img_5.png](screenshots/img_4.png)
    
    Найти переменную Path, изменить, добавить в конец: %JAVA_HOME%\bin
![img_6.png](screenshots/img_5.png)

## Как запустить программу
1)	Создаем папку, например, testHistory (или другое название) в C:\Users\Public (общая папка), где хотим разместить программу.
2)	В пути C:\Users\Public\testHistory создаем папку execCopy – здесь будет храниться программа копирования истории.
3)	В пути C:\Users\Public\testHistory создаем папку execAnalyzer – здесь будет храниться программа анализатора истории.
4)	Файл HistoryCopyMaker-1.0-SNAPSHOT.jar помещаем в C:\Users\Public\testHistory\execCopy, так же внутри создаем startCopyMaker.bat файл(для запуска программы), который содержит:
```shell
start javaw -jar HistoryCopyMaker-1.0-SNAPSHOT.jar <path_to_created_folder> <browser_type> <logging_option>
```
    path_to_created_folder - путь до созданной папки C:/Users/Public/<created_folder>
    browser_type - тип браузера "chrome" или "firefox" (по умолчанию, "chrome", если не указывать)
    logging_option - включение логирования в файл C:/Users/Public/<created_folder>/log/<username>_copyMaker.log. 
                    Параметр необязательный, указывать возможно при указании browser_type. Чтобы включить нужно передать "log_on". 
                    Если указать другую строку логинг будет выключен.

5)	Дальше создаем ярлык файла startCopyMaker.bat (правая кнопка мыши – создать ярлык) и помещаем ярлык в папку Автозагрузки (нажимаем и переносим в папку, что изображена на скриншоте):
      C:\ProgramData\Microsoft\Windows\Start Menu\Programs\Startup
![img_7.png](screenshots/img_7.png)

# Вспомогательные ссылки:
1)	https://stackoverflow.com/questions/3333553/how-can-i-change-the-java-runtime-version-on-windows-7
2)	https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html#license-lightbox
