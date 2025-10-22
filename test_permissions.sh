#!/bin/bash

echo "🔧 Android 14 MediaSession权限测试脚本"
echo "================================="
echo ""

echo "📱 检查设备信息..."
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
echo ""

echo "🔍 检查当前权限状态..."
adb shell dumpsys package com.max.carlaunchersimulator | grep -A 10 "requested permissions:"
echo ""

echo "📋 检查MEDIA_CONTENT_CONTROL权限..."
adb shell pm list permissions | grep MEDIA_CONTENT_CONTROL
echo ""

echo "🎵 检查音乐App是否在运行..."
adb shell ps | grep media_center
echo ""

echo "🔧 重新安装App并测试..."
echo "1. 卸载旧版本..."
adb uninstall com.max.carlaunchersimulator
echo ""

echo "2. 安装新版本..."
./gradlew installDebug
echo ""

echo "3. 启动App并查看权限申请..."
adb shell am start -n com.max.carlaunchersimulator/.MainActivity
echo ""

echo "4. 查看Logcat输出..."
echo "等待5秒后查看日志..."
sleep 5
adb logcat -d | grep -E "(🔐|✅|❌|🔍|📱|🎵|🔗|MEDIA_CONTENT_CONTROL)" | tail -20
echo ""

echo "📝 如果权限申请失败，尝试手动授予权限："
echo "adb shell pm grant com.max.carlaunchersimulator android.permission.MEDIA_CONTENT_CONTROL"
echo ""

echo "🧪 测试完成！"
