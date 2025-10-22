# 测试音乐播放器App

## 问题分析

你的车机桌面控件无法连接到音乐App，主要原因是：

1. **目标App不存在**: 代码中硬编码了 `com.max.media_center`，但这个App可能不存在
2. **权限问题**: `MEDIA_CONTENT_CONTROL` 权限需要用户手动授权
3. **MediaSession支持**: 目标App需要有MediaSession支持

## 解决方案

### 方案1: 使用系统音乐App测试

1. **安装一个支持MediaSession的音乐App**，比如：
   - Spotify
   - Google Play Music
   - 网易云音乐
   - QQ音乐

2. **启动音乐App并开始播放音乐**

3. **运行车机桌面控件App**，应该能看到连接信息

### 方案2: 修改代码连接系统音乐App

修改 `MainActivity.kt` 中的包名：

```kotlin
// 改为连接系统音乐App
val intent = packageManager.getLaunchIntentForPackage("com.android.music")
// 或者
val intent = packageManager.getLaunchIntentForPackage("com.google.android.music")
```

### 方案3: 创建简单的测试音乐App

如果你想要一个完全可控的测试环境，我可以帮你创建一个简单的音乐播放器App。

## 调试步骤

1. **查看Logcat输出**，寻找以下信息：
   ```
   🔍 开始搜索活跃的MediaSession...
   📱 找到 X 个活跃的MediaSession
   🎵 MediaSession 0: com.xxx.music
   ```

2. **检查权限**：
   - 确保授予了 `MEDIA_CONTENT_CONTROL` 权限
   - 在设置中手动检查权限状态

3. **确保音乐App正在播放**：
   - MediaSession只有在播放时才会活跃
   - 暂停的音乐App不会出现在活跃会话中

## 当前状态

- ✅ 权限申请逻辑已添加
- ✅ 调试信息已添加
- ✅ 错误处理已完善
- ❌ 需要目标音乐App支持MediaSession
