# FastBle 项目构建说明

## 环境要求

| 工具 | 最低版本 |
|------|----------|
| Android Studio | Ladybug (2024.2) 或更高 |
| JDK | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.5.0 |

## 项目结构

```
FastBle/
├── app/                  # 示例 Demo 应用
├── FastBleLib/           # BLE 核心库（Android Library）
├── build.gradle          # 根构建脚本，声明 AGP 插件版本
├── config.gradle         # 统一版本配置（SDK 版本、版本号）
├── settings.gradle       # 模块声明与仓库配置
├── gradle.properties     # Gradle 全局属性
└── gradle/wrapper/       # Gradle Wrapper
```

## SDK 版本配置

所有版本号统一在 `config.gradle` 中管理：

| 参数 | 值 |
|------|----|
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 21 |
| versionCode | 250 |
| versionName | 2.5.0 |

## 模块说明

### FastBleLib

BLE 核心库模块，输出为 AAR。

- **namespace**: `com.clj.fastble`
- **类型**: Android Library
- **依赖**: 无外部依赖

构建 AAR 文件：

```bash
./gradlew :FastBleLib:makeAAR
```

生成路径：`FastBleLib/build/aarFloder/FastBLE-{version}.aar`

### app

示例 Demo 应用，演示 FastBleLib 的使用方式。

- **namespace**: `com.clj.blesample`
- **applicationId**: `com.clj.blesample`
- **类型**: Android Application
- **依赖**: FastBleLib、AndroidX AppCompat

## 构建命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 构建全部
./gradlew assemble

# 清理构建产物
./gradlew clean
```

## 蓝牙权限说明

项目已适配 Android 12 (API 31) 引入的新蓝牙权限模型：

| 权限 | 适用范围 |
|------|----------|
| `BLUETOOTH` | Android 11 及以下（maxSdkVersion=30） |
| `BLUETOOTH_ADMIN` | Android 11 及以下（maxSdkVersion=30） |
| `BLUETOOTH_SCAN` | Android 12+ |
| `BLUETOOTH_CONNECT` | Android 12+ |
| `ACCESS_FINE_LOCATION` | 全版本（BLE 扫描需要） |
| `ACCESS_COARSE_LOCATION` | 全版本 |

> **注意**: 在 Android 6.0+ 上，`ACCESS_FINE_LOCATION` 需要运行时动态申请。在 Android 12+ 上，`BLUETOOTH_SCAN` 和 `BLUETOOTH_CONNECT` 也需要动态申请。

## 注意事项

- Java 编译级别为 **Java 17**，请确保 Android Studio 使用的 JDK 版本 >= 17。
- 项目使用 **nonTransitiveRClass**，R 类字段为非常量，不能在 `switch-case` 中使用，需使用 `if-else` 替代。
- 仓库配置集中在 `settings.gradle` 的 `dependencyResolutionManagement` 中，子模块不应单独声明 `repositories`。
