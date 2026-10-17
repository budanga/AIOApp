# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Minesweeper saves games as JSON with Gson 2.8.5, which ships no R8 rules.
# Keep generic signatures (List<Cell>) and field names so saved games deserialize, also across updates.
-keepattributes Signature
-keepclassmembers class com.example.aioapp.ui.minesweeper.SavedGame { <fields>; }
-keepclassmembers class com.example.aioapp.ui.minesweeper.Cell { <fields>; }
-keepclassmembers class com.example.aioapp.ui.minesweeper.Difficulty { <fields>; }
-keep enum com.example.aioapp.ui.minesweeper.GameState { *; }
