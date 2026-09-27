# ICAI Batch Checker R8 rules.
# Room (KSP-generated), OkHttp and Jsoup all ship correct consumer rules; no
# broad -keep of third-party code is needed or wanted.

# Readable crash traces in the field.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
