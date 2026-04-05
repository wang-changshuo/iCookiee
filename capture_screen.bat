@echo off
adb -s emulator-5554 shell screencap /sdcard/screen.png
adb -s emulator-5554 pull /sdcard/screen.png D:\Downloads\work\ai_finance_app\current_screen.png
echo Screenshot saved!