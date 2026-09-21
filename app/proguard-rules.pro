# R8 / ProGuard rules for the release build.
#
# The defaults come from proguard-android-optimize.txt (see app/build.gradle.kts); this file
# holds anything specific to this app. Keep it small — every rule here is shrinking and
# optimisation we give up, so only add one when a release build actually needs it.
#
# Before adding a keep rule, check app/build/intermediates/aapt_proguard_file/: AGP already
# generates precise rules for classes named in AndroidManifest.xml and in layout XML, which
# covers our Activities and the custom Views (TerminalMapView, InteractiveTerminalMapView).
# A broad rule like "-keep public class * extends android.view.View" is not only redundant
# with those, it also pins every View in appcompat and material and costs ~1 MB in the APK.

# Keep line numbers and map them back to the original file so release crash reports stay
# readable. The mapping file for each build is written to
# app/build/outputs/mapping/release/mapping.txt — upload it with the release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# NOTE ON JSON PARSING
# Airport data is parsed by hand with org.json (see AirportRepository), reading fields by
# string key, so no model class is touched reflectively and no keep rules are needed for
# com.tunnellight.airport_terminal.model.
#
# If a reflection-based parser (Gson, Moshi, kotlinx-serialization) is ever introduced, the
# model classes MUST be kept here or release builds will silently deserialise to null/empty
# while debug builds keep working. Something like:
#
#   -keep class com.tunnellight.airport_terminal.model.** { *; }
