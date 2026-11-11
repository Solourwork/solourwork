# 同频 PULSE Android 1.1.1 验证记录

日期：2026-11-11。包名 `work.solour.pulse`，versionCode12 / versionName1.1.1，minSdk26（Android8.0），targetSdk32。Linux、Temurin JDK21.0.9+10、Android platform35 / build-tools35.0.0。

## 本轮已通过

1. **算法与会话逻辑**：35条纯Java断言。涵盖HR8/16位解析、过期与暂停、速度积分和估算、器械推测/确认/删除、音乐切换缓冲及休息节点。
2. **Android9框架层交互模拟**：Robolectric4.11.1 / JUnit4.13.2，4个用例，140次控件操作、373条断言通过。直接对生产MainActivity的Android View分发触摸，验证导航和实际业务/存储状态；弹窗确认使用真实Button点击处理。不是对业务另写一套网页或返回固定成功的替代实现。
3. **Android14框架与Skia渲染**：1个用例通过，渲染11张生产界面预览，检查模拟/未授权蓝牙/已授权蓝牙时的前台服务类型选择。预览来自生产Activity和View代码，经Android原生绘制接口生成；不是设计稿，也不是启动完整Android虚拟机后的屏幕截图。
4. **APK编译与签名**：编译、DEX、打包、4字节对齐、apksigner v2/v3验证通过。沿用1.1的RSA3072签名，证书SHA256为 `c5d789b8aed7093ac13fa8b6aabe97775c6c9914360e3740ec1fcfcd024ad1fa`。
5. manifest没有INTERNET权限，健康报告不上传。官网工作区未改，main仍为 `a9664c744c5b263490ea7d013ebed5ce332ef04b`。

## 交互回归覆盖

- 三个场景入口、空记录页、我的、关于、页面返回。
- 音乐开关即时状态及本地保存，三种语音风格，系统TTS设置Intent。
- 目标、里程/时间选择、无效输入、休息30–600秒范围、手动估算速度、心率参考值。
- 真实/模拟模式选择；扫描按钮的等待授权、拒绝、授权后继续或显示硬件不可用原因、扫描控件恢复。
- 三场景开始、计时推进、暂停冻结、继续，返回首页后恢复进行中的训练。
- 休息暂停、延长、600秒上限、返回时保留倒计时，到时只提醒不启动下一组。
- 器械手动起止、空组页、暂存推测组确认/删除、丢弃未完成组、结束排除未完成组。
- 结束确认/取消、后台保存、报告查看/返回、历史列表、删除确认/取消和持久化结果。

为了可重复验证组确认界面，测试中直接建立待确认组作为夹具；自动判组算法另由纯Java测试验证。不能据此宣称真实心率可以准确判组。

## 整机模拟器与真机限制（未通过的项目）

本轮**没有完成新APK的整机模拟器全流程验收**。当前云机器没有/dev/kvm；在软件CPU模拟下，Emulator37.3.8 / Android9 x86_64出现System UI无响应和ADB通信停滞；改用Emulator33.1.24、ADB35.0.2以及干净Android9 x86设备后，仍未得到稳定的完整运行结果。底层原因未确认。整机UiRunner保留在源码中，以便在有稳定模拟器的机器重跑；其未完成结果不计作通过。上一版的25条设备内断言也不作为本轮新APK的通过证明。

框架模拟会替代设备系统服务和硬件，不能证明真实的蓝牙连接、系统权限弹窗、锁屏长期采集、来电/耳机切换或音频播放已经通过整机测试。实体BLE、华为/小米设备、中文离线TTS发音、音乐听感、厂商省电策略均**未经真机验证**，不承诺全型号兼容。

## 复现

先按README运行 `scripts/build.sh`。然后准备Maven、Python3、curl、ripgrep和相同SDK/JDK环境，运行 `bash scripts/framework-test.sh`；依赖版本由pom.xml固定，Android14 SDK jar用于原生渲染。生成 `build/framework-test.log`、`build/native-preview-test.log`、`build/native-previews/`。

在单独测试用模拟器安装APK，设置ADB及同一签名环境后运行 `TEST_RUNNER=UiRunner bash scripts/device-test.sh` 可进行整机触摸回归；该用例操作模拟器内报告和偏好，勿直接用于有重要数据的个人手机。它与框架层测试是两个不同级别的验证。

## 交付

交付位于独立 `pulse-ui-refinement` 分支的 `deliverables/1.1.1/`：源码归档先持久保存，再保存APK、校验文件、框架测试记录、构建签名记录和11张原生控件预览。预览目录明确标注为框架渲染。

本轮不覆盖原1.1附件、不修改官网、不向main推应用代码、不发布GitHub Release。具体下载文件以SHA256SUMS为准。
