# 同频 PULSE · 1.2 UI 与交互体验稿

本分支为 `pulse-companion-1.2`，将产品重心调整为**运动音乐与语音伴侣**：日常一键开始、耳机里持续音乐、有足够变化信号时低频问一句、简单回复或保持沉默。前台取消训练项目选择、里程目标、手动记组与训练成绩主线。

用户已明确要求语音必须离线。本体验稿的真实语音识别与运动变化感知**尚未实现**；问答为明显标注的脚本演示，没有麦克风和网络权限，不实际收音。音乐播放、标准BLE心率参考与媒体播放/暂停回调有代码接入，但本轮没有用真实手机和耳机验证。

- [完整产品与交互设计](COMPANION-DESIGN.md)
- [验证范围与未实现能力](TESTING.md)
- 交付目录：`deliverables/1.2-ui-preview/`

## 页面

炭黑、燃橙、玫红与电紫；首页一个主开始按钮，陪伴中一个主暂停按钮。询问、等待和回复反馈处于同一界面，完整设计不要求触屏确认。偏好仅保留音乐、离线语音设计、可选设备与隐私。

系统离线中文TTS可用时，演示会先播完问题再进入字幕回复阶段；缺失时使用字幕脚本。演示中的“对”是脚本，不是实际识别的用户声音，也不会改变正在进行的真实音乐会话。

## 构建

Linux、JDK21、Android platform35 / build-tools35.0.0、zip、Python3。minSdk26（Android8+），targetSdk32，为GitHub侧载体验交付，不是应用商店发布。

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_JAR=/path/to/sdk/platforms/android-35/android.jar
export BUILD_TOOLS=/path/to/sdk/build-tools/35.0.0
# 可选：通过私密环境变量提供SIGNING_KEYSTORE、SIGNING_PASSWORD，alias为pulse
bash scripts/build.sh
```

输出：`dist/pulse-1.2.0-ui-preview.apk`。沿用1.1/1.1.1的签名，versionCode13，versionName1.2.0-preview。通过入口别名保留旧的MainActivity启动组件，实际目标是新的PulseCompanionActivity。旧训练UI代码保留在历史源中，不是本版启动界面。旧版报告文件不删除，新陪伴会话不保存里程或组数成绩。

## UI 与策略验证

```sh
# 先运行上述构建，另需Maven、curl、ripgrep
bash scripts/framework-test.sh
```

脚本下载固定版本的Robolectric / JUnit等测试依赖，在Android14框架与Skia中检查生产View、控件操作、页面返回、播放控制回调和明确标注的脚本演示，输出原生控件渲染预览。这个过程不代表启动整台Android虚拟机或完成真实耳机识别测试。

构建脚本另运行35条遗留基础逻辑回归及30条问答策略原型断言；这些不是运动状态识别或ASR准确率测试。旧版1.1.1的工具和记录仍可在其独立版本与历史源码中找到，不作为本版新能力的完成证明。

## 能力边界

尚未实现：真实运动变化推测、手机步数接入、离线中文回复识别、耳机麦克风收音、锁屏语音闭环、常驻唤醒词。厂商私有SDK、个人曲库、云端大模型鼓励、真人音色亦未实现。华为/小米是适配目标，不能承诺全型号兼容。

健康数据不上传；无账号；不使用未经授权的公众人物音色。原始录音在本体验稿中不存在。完整语音版本将使用随包提供的本地模型和有限回复词表，问完才开短时收音，沉默/不确定不确认，绝不使用联网识别兜底。

源码先归档持久保存，再保存APK和校验文件。官网与main不修改，本轮不发布GitHub Release。
