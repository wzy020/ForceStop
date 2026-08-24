# ForceStop

一个简单的 Android 应用，用于查看正在运行的 App 并批量强制停止它们。

> 本应用需要 **root 权限** 才能执行 `am force-stop` 与 `am start` 等命令。

- 最低支持 Android 14（API 34），编译目标 API 35。
- 跳转「正在运行的服务」页面依赖各厂商系统的实现（如 ColorOS 为 `OplusRunningServices`，AOSP 为 `ProcessStatsSummary`），通过 root 的 `am start` 启动。
