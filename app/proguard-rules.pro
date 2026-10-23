# Workers are created by reflection.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
