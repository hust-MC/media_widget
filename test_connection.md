# MediaSession连接测试指南

## 🔍 模拟器问题

你遇到的错误是Android模拟器的**fs-verity**文件系统问题，与我们的MediaSession代码无关。

## 🛠️ 解决方案

### 方案1: 重启模拟器
1. 关闭当前模拟器
2. 重新启动模拟器
3. 重新测试连接

### 方案2: 创建新的模拟器
1. 打开Android Studio
2. 进入 AVD Manager
3. 创建新的Android 14模拟器
4. 使用新模拟器测试

### 方案3: 使用真机测试
1. 连接Android真机
2. 启用USB调试
3. 在真机上测试

## 🧪 测试步骤

### 1. 启动音乐App
```bash
cd ../media_center
./gradlew installDebug
# 在设备上启动音乐App并开始播放
```

### 2. 启动车机桌面控件
```bash
cd ../CarLauncherSimulator
./gradlew installDebug
# 在设备上启动车机桌面控件App
```

### 3. 查看连接状态
```bash
adb logcat | grep -E "(🔍|✅|❌|MediaBrowser|MediaController)"
```

## 📱 预期的Logcat输出

**成功连接时**:
```
🔍 尝试使用MediaBrowser连接音乐App...
✅ MediaBrowser连接成功！
✅ MediaController设置成功！
```

**连接失败时**:
```
🔍 尝试使用MediaBrowser连接音乐App...
❌ MediaBrowser连接失败
```

## 🔧 代码修改说明

我们已经修改了连接方式：
- **旧方式**: 使用`MediaSessionManager.getActiveSessions()` (需要特殊权限)
- **新方式**: 使用`MediaBrowser`连接`MediaBrowserService` (不需要特殊权限)

这种方式绕过了Android 14的权限限制问题，应该能够正常工作。

## 📝 注意事项

1. **确保音乐App正在播放**: MediaSession只有在播放时才会活跃
2. **检查包名**: 确保音乐App的包名是`com.max.media_center`
3. **查看错误信息**: 如果连接失败，查看具体的错误信息
