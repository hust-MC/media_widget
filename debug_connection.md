# MediaSession连接调试指南

## 问题分析

`MEDIA_CONTENT_CONTROL`权限在Android 11+上是**系统级权限**，普通应用无法通过`requestPermissions`申请。

## 解决方案

### 1. 权限处理
- 移除了无法申请的`MEDIA_CONTENT_CONTROL`权限
- 直接尝试连接MediaSession，通过异常处理权限问题
- 添加了详细的调试信息

### 2. 测试步骤

1. **确保音乐App正在播放**:
   ```bash
   # 启动media_center项目
   cd ../media_center
   ./gradlew installDebug
   # 在手机上启动音乐App并开始播放
   ```

2. **启动车机桌面控件**:
   ```bash
   # 启动CarLauncherSimulator项目
   cd ../CarLauncherSimulator
   ./gradlew installDebug
   ```

3. **查看Logcat输出**:
   ```bash
   adb logcat | grep -E "(🔧|✅|❌|🔍|📱|🎵|🔗)"
   ```

### 3. 预期的Logcat输出

**成功连接时**:
```
🔧 初始化MediaSessionManager...
✅ MediaSessionManager初始化成功
🔍 开始搜索活跃的MediaSession...
📱 找到 1 个活跃的MediaSession
🎵 MediaSession 0: com.max.media_center
🔗 连接到: com.max.media_center
✅ MediaSession连接成功！
```

**权限不足时**:
```
🔧 初始化MediaSessionManager...
✅ MediaSessionManager初始化成功
🔍 开始搜索活跃的MediaSession...
❌ 权限不足: java.lang.SecurityException: ...
```

### 4. 如果还是连不上

1. **检查音乐App是否在播放**:
   - MediaSession只有在播放时才会活跃
   - 暂停的音乐App不会出现在活跃会话中

2. **检查Android版本**:
   - Android 11+需要特殊权限处理
   - 可能需要手动在设置中授予权限

3. **尝试重启设备**:
   - 有时权限缓存会导致问题

### 5. 手动授予权限（如果可能）

在Android设置中：
1. 进入"应用管理"
2. 找到"车机桌面控件"
3. 查看"权限"选项
4. 手动授予媒体控制权限（如果可用）
