if ! [ $1 ]; then
  echo 'start build..'
  ./gradlew assembleDebug
fi

packageName="com.aliyun.interaction.app"

#echo 'clear app data..'
#adb shell pm clear $packageName

echo 'start install..'
adb install -t -r app/build/outputs/apk/debug/app-debug.apk

echo 'start setup..'
adb shell am start $packageName/$packageName.MainActivity
