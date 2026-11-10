# Android 1.1 验证记录

构建日期：2026-11-10。JDK Temurin 21.0.9+10、Android platform35、build-tools35.0.0；APK min26 / target32。

## 已完成

- `EngineTest`：35项纯Java算法/会话流程断言通过。覆盖8/16位HR及短包、10秒心率过期、5秒速度过期、手动速度估算、暂停冻结、断连不模拟、20秒基线、开始/结束阈值及持续时间、待确认/确认/删除、警示/暂停阻断推测、未完成组排除、两类音乐缓冲/最小间隔、休息播报节点/暂停/不启动下一组、180秒鼓励间隔。
- `FlowRunner`：Android9（API28）x86_64软件模拟器内25项集成断言通过。三场景前台服务开始、显式模拟、有效样本、暂停、结束及报告持久化、里程、待确认组、未完成组、100份上限和删除、Activity启动。最终日志见验证附件。
- APK编译、DEX打包、4字节对齐通过，`apksigner verify`验证v2/v3签名通过。RSA3072新签名，证书SHA256：`c5d789b8aed7093ac13fa8b6aabe97775c6c9914360e3740ec1fcfcd024ad1fa`。
- manifest无INTERNET权限；备份关闭。主站仓库工作区无修改，应用仅在独立分支。

## 验证限制与观察

没有实体手机、运动设备或BLE外设。华为/小米标准服务开放程度、真实BLE8/16位连接、RSC/CSC通知、厂商省电策略、长时锁屏采集、来电/耳机切换、音乐交叉渐变听感和中文离线TTS可用性均**未经真机验证**。模拟器显示系统TTS初始化失败，故未验证中文离线语音可用性、中文发音或总结听感。

没有/dev/kvm，使用强制TCG软件模拟。初次批量报告测试/开机阶段观察到应用和System UI无响应弹窗。将测试改为种子数据验证100份上限，避免在一个UI任务中循环102次保存；最终25项集成断言通过，首页UI层级成功读取且无无响应弹窗。首页及单车模拟训练页已截图目视检查；训练页持续动态更新导致uiautomator无法获得idle状态，使用截图检查。不证明生产环境性能、全部UI路径或长时运行已通过验证。纯算法测试不等同实机验证；自动判组仍标为未经实机验证的推测。

## 复现

`bash scripts/build.sh`运行算法测试并构建。先安装签名APK，在同一签名配置和 `ADB` 指向已启动设备后运行 `bash scripts/device-test.sh`，构建并安装独立instrumentation测试APK。

## 发布状态

源码和归档优先持久保存到GitHub独立分支 `pulse-android-1.1`。GitHub API返回管理员/写权限，创建Release草稿成功；但 `uploads.github.com` 上传附件返回401 Bad credentials。附件上传未成功，不将空草稿标为正式发布。不提供不存在的Release APK下载链接。需要为uploads.github.com配置有效的目标仓库Contents:write凭据后重试。
