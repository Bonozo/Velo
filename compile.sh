#/opt/android-ndk-r8d/ndk-build clean
/opt/android-ndk-r8d/ndk-build 

cp native/ffmpeg/ffmpeg-0.11.1/android/armv6_vfp/libffmpeg.so libs/armeabi/.
cp native/ffmpeg/ffmpeg-0.11.1/android/armv7-a/libffmpeg.so libs/armeabi-v7a/.
cp native/ffmpeg/ffmpeg-0.11.1/android/x86/libffmpeg.so libs/x86/.
cp native/ffmpeg/ffmpeg-0.11.1/android/mips/libffmpeg.so libs/mips/.

cp native/opencv2.4.3.2/native/libs/armeabi/* libs/armeabi/
cp native/opencv2.4.3.2/native/libs/armeabi-v7a/* libs/armeabi-v7a/
cp native/opencv2.4.3.2/native/libs/mips/* libs/mips/
cp native/opencv2.4.3.2/native/libs/x86/* libs/x86/

#cp native/ffmpeg/ffmpeg-0.11.1/android/armv5te/libffmpeg.so libs/armeabi/.
