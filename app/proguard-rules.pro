# Simpan info file/line untuk crash report
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable