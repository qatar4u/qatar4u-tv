# الاسترجاع · Restore

## بالعربية

1. وصّل التابلت بالكمبيوتر بالكابل، وتأكد أن `adb devices` يظهر التابلت بجانبه كلمة `device`.
2. انظر إلى حالة الجهاز أولاً (انظر README_AR.md).
3. **واجهة سامسونج:** نفّذ القسم 1 من `ADB_Recovery_Commands.txt`. أو من التابلت نفسه: الإعدادات ← التطبيقات ← اختيار التطبيقات الافتراضية ← تطبيق الشاشة الرئيسية ← One UI Home.
4. **التطبيقات المعطّلة:** نفّذ أوامر القسم 2 سطراً سطراً. أو من التابلت: الإعدادات ← التطبيقات ← اختر التطبيق ← تمكين.
5. **الإعدادات:** نفّذ القسم 3.
6. **إن أردت حذف Qatar4u TV:** القسم 4. هذان تطبيقان ثبّتهما أنت، وحذفهما لا يمس النظام.
7. شغّل أوامر القسم 5 وتأكد أن القائمة المعطّلة فارغة من تطبيقات سامسونج، وأن الشاشة الرئيسية `com.sec.android.app.launcher`.

### بعد الانتهاء: أغلق ADB

1. على التابلت: الإعدادات ← خيارات المطوّر ← **إلغاء تصاريح تصحيح أخطاء USB**.
2. أطفئ **تصحيح أخطاء USB**.
3. إن أردت، أطفئ **خيارات المطوّر** نفسها من أعلى الصفحة.
4. أعد تشغيل التابلت.

## In English

1. Connect the tablet by cable and check that `adb devices` lists it as `device`.
2. Look at the current state first (see README_EN.md).
3. **Samsung home screen:** run section 1 of `ADB_Recovery_Commands.txt`, or on the tablet: Settings > Apps > Choose default apps > Home app > One UI Home.
4. **Disabled apps:** run the section 2 lines one at a time, or on the tablet: Settings > Apps > the app > Enable.
5. **Settings:** run section 3.
6. **To remove Qatar4u TV:** section 4. Both are apps you installed; removing them does not touch the system.
7. Run section 5: no Samsung app should be listed as disabled, and Home should be `com.sec.android.app.launcher`.

### When you are done: close ADB

1. On the tablet: Settings > Developer options > **Revoke USB debugging authorisations**.
2. Turn **USB debugging** off.
3. If you like, turn **Developer options** off at the top of that page.
4. Restart the tablet.
