# النسخ الاحتياطي · Backup

## بالعربية

1. **إعدادات Qatar4u TV:** افتح الإعدادات ← النسخ الاحتياطي ← احفظ الإعدادات في ملف، واختر مكان الحفظ. الملف يحفظ المدينة وطريقة الحساب والتعديلات والخلفيات المثبّتة وترتيب التطبيقات. الرمز السري لا يُحفظ في الملف.
2. **قائمة التطبيقات الحالية:** من الكمبيوتر، في مجلد platform-tools:
   ```
   adb shell pm list packages -f > packages-now.txt
   adb shell pm list packages -d > disabled-now.txt
   ```
3. **صورك وملفاتك:** لا تتأثر بالتراجع، لكن انسخها إلى الكمبيوتر إن أردت الاطمئنان.

## In English

1. **Qatar4u TV settings:** Settings > Backup & restore > Save settings to a file. The file holds the city, the prayer method and offsets, the pinned wallpaper and the app order. The PIN is never written to it.
2. **The current package list**, from the platform-tools folder on the computer:
   ```
   adb shell pm list packages -f > packages-now.txt
   adb shell pm list packages -d > disabled-now.txt
   ```
3. **Your photos and files** are not touched by a rollback; copy them to the computer if you want to be sure.
