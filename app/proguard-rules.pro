# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /media/pgp/Data/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontobfuscate
-optimizationpasses 5

# Remove Log.d messages
-assumenosideeffects class android.util.Log {
    public static int d(...);
}

# Apache Commons
-keep class org.apache.http.**
-keep interface org.apache.http.**

-dontwarn org.apache.commons.**

# Apache mina
-dontwarn javax.security.sasl.*
-dontwarn org.ietf.jgss.*
-dontwarn org.apache.mina.core.session.DefaultIoSessionDataStructureFactory$DefaultIoSessionAttributeMap #Java 8 not implememnted
-dontwarn org.apache.mina.util.ExpiringMap #Java 8 not implememnted
-keepclassmembers class * implements org.apache.mina.core.service.IoProcessor {
    public <init>(java.util.concurrent.ExecutorService);
    public <init>(java.util.concurrent.Executor);
    public <init>();
}

# jcifs
-dontwarn javax.servlet.**
-dontwarn jcifs.http.NetworkExplorer

-keep,allowoptimization,allowobfuscation class eu.masconsult.android_ntlm.* {*;}

# Apache ftpsever
-dontwarn org.apache.ftpserver.**

#From here SpongyCastle (https://github.com/signalapp/Signal-Android/blob/master/proguard-spongycastle.pro)
-keep class org.spongycastle.crypto.* {*;}
-keep class org.spongycastle.crypto.agreement.** {*;}
-keep class org.spongycastle.crypto.digests.* {*;}
-keep class org.spongycastle.crypto.ec.* {*;}
-keep class org.spongycastle.crypto.encodings.* {*;}
-keep class org.spongycastle.crypto.engines.* {*;}
-keep class org.spongycastle.crypto.macs.* {*;}
-keep class org.spongycastle.crypto.modes.* {*;}
-keep class org.spongycastle.crypto.paddings.* {*;}
-keep class org.spongycastle.crypto.params.* {*;}
-keep class org.spongycastle.crypto.prng.* {*;}
-keep class org.spongycastle.crypto.signers.* {*;}

-keep class org.spongycastle.jcajce.provider.asymmetric.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.util.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.dh.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.ec.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.rsa.* {*;}

-keep class org.spongycastle.jcajce.provider.digest.** {*;}
-keep class org.spongycastle.jcajce.provider.keystore.** {*;}
-keep class org.spongycastle.jcajce.provider.symmetric.** {*;}
-keep class org.spongycastle.jcajce.spec.* {*;}
-keep class org.spongycastle.jce.** {*;}

#From here BouncyCastle
-keep class org.bouncycastle.crypto.* {*;}
-keep class org.bouncycastle.crypto.agreement.** {*;}
-keep class org.bouncycastle.crypto.digests.* {*;}
-keep class org.bouncycastle.crypto.ec.* {*;}
-keep class org.bouncycastle.crypto.encodings.* {*;}
-keep class org.bouncycastle.crypto.engines.* {*;}
-keep class org.bouncycastle.crypto.macs.* {*;}
-keep class org.bouncycastle.crypto.modes.* {*;}
-keep class org.bouncycastle.crypto.paddings.* {*;}
-keep class org.bouncycastle.crypto.params.* {*;}
-keep class org.bouncycastle.crypto.prng.* {*;}
-keep class org.bouncycastle.crypto.signers.* {*;}

-keep class org.bouncycastle.jcajce.provider.asymmetric.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.util.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.dh.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.ec.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.* {*;}

-keep class org.bouncycastle.jcajce.provider.digest.** {*;}
-keep class org.bouncycastle.jcajce.provider.keystore.** {*;}
-keep class org.bouncycastle.jcajce.provider.symmetric.** {*;}
-keep class org.bouncycastle.jcajce.spec.* {*;}
-keep class org.bouncycastle.jce.** {*;}

-dontwarn javax.naming.**

# SSHJ
-keep class net.schmizz.sshj.** {*;}
