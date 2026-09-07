# 拾课

供个人使用的安卓课程表。原生 Java 应用，支持 Android 8.0 及以上，目标适配 Android 16。

## 安装与首次使用

1. 将 `outputs/shike-0.1.2.apk` 和学校导出的课表文件发送到手机，点击 APK 安装。若系统询问，允许当前文件管理器安装此应用。
2. 打开「拾课」→「导入」→「选择课表文件」，选择学校导出的 `.xls`。
3. 检查识别预览，点击课程可以编辑或排除，最后确认导入。学期起始日期需要正确；中南大学 2026-2027-1 的第1周从 2026-09-06（周日）开始。
4. 「课表」切换教学周；点击课程查看时间、教室、教师并编辑。「今日」显示当天课程。
5. 「设置」可修改学期、作息、浅色/深色主题，也可申请添加两个桌面小组件。或长按手机桌面→小组件→拾课。

## 已实现功能

- 本地周课表、今日课程、添加/编辑/删除、学期起始日与作息设置。
- 连续周、分段周、单双周；教师选填。同名课程的不同上课时间分别保存。
- XLS/XLSX、DOCX、PDF和UTF-8文本导入。所有来源先预览，不直接覆盖已有课表；合并时对相同课程安排去重。
- 文字版PDF按文字坐标还原表格，扫描页使用内置中文OCR。本地处理，无需配置模型密钥。
- 中南大学门户内置浏览器，登录后读取本科课表；不向网页开放原生代码接口。
- 今日课程与下一节课组件；支持系统浅色/深色，点击打开应用，保存课程后更新。
- 一次性保存课程、学期和作息；待确认预览单独保存，可在应用重建后继续校对。

## 导入边界

| 来源 | 当前支持与限制 |
| --- | --- |
| 学校导出的XLS | 已用提供的文件验证21项上课安排、10项作息、学期及起始日；文件本身没有教师信息 |
| XLSX | 读取首个工作表的单元格与共享文本；复杂跨行合并布局需核对 |
| DOCX | 课程表格或含明确星期、节次、周次的段落；纵向合并会提示校对 |
| 旧版DOC | 请先用Word另存为DOCX或PDF |
| PDF | 文字层优先，扫描件中文OCR兜底；最多30页、其中最多12页OCR，文件32MB以内 |
| 复杂扫描表格 | 倾斜、多张表混排、跨页课程、合并单元格或缺少表头可能无法完整恢复，需手动补充或改用Excel |
| 教务网页 | 当前为中南大学本科系统专用。网页读取链路已通过模拟页面测试；真实手机统一认证及学校网络访问仍需实机验证 |

识别器不会补造教师信息。OCR漏字时可能把数值范围暂作周次，相关警告会明确要求确认。实践/备注中没有星期或节次的安排不能直接排入某节课，需要补充。

小组件由安卓系统约每30分钟请求更新，保存或打开App时也更新。系统休眠或后台管理可能推迟刷新，本版本不提供精准到分钟的上下课提醒。

课程保存在手机应用私有空间。应用卸载会清除本地课程，保留原始课表文件可以重新导入。安装包不预装个人课表、学号或登录凭据。

## 构建

本项目使用 JDK17、Gradle8.13、AGP8.11.1、Android SDK36。项目通过 Gradle Wrapper 固定版本并校验分发包哈希。

```powershell
./scripts/build.ps1 -Tasks @(':core:test', ':app:assembleDebug', ':app:lintDebug')
```

当前机器的构建脚本会选用 `H:/android-toolchain/jdk17` 和 `H:/android-toolchain/sdk`。换机时设置 `JAVA_HOME`、`ANDROID_HOME`，并在未提交的 `local.properties` 中填写SDK路径。Windows中文路径使用目录联接作为编译入口，源码仍在原目录。

输出为 `app/build/outputs/apk/debug/app-debug.apk`，自带调试签名，可直接安装。个人使用无需应用商店。

## 测试

`core` 是纯 Java 模块，覆盖日期周次、单双周、Excel/Word/HTML解析与PDF几何布局。

Android仪器测试位于 `app/src/androidTest`，使用 Android16 模拟器验证真实XLS、本地保存、预览、主题、DOCX、文字/扫描PDF、WebView读取及小组件绑定刷新。个人XLS样本被Git忽略，不进入正式APK。运行前将测试用XLS复制为 `app/src/androidTest/assets/private-schedule.xls`；测试使用独立的模拟器，不应在自己的日用手机上运行，会覆盖该模拟器中的测试应用数据。

PDF测试样本不含个人信息，可用 `scripts/make_pdf_fixtures.py` 重新生成。

```powershell
./scripts/build.ps1 -Tasks @(':app:assembleDebug', ':app:assembleDebugAndroidTest')
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w cn.edu.csu.timetable.test/androidx.test.runner.AndroidJUnitRunner
```

## 目录

- `core`：排课模型、周次与文件/表格解析。
- `app`：安卓界面、文件读取、OCR、WebView、小组件和本地保存。
- `docs`：确认方案、实施计划与验证记录。
- `outputs`：供安装的APK与界面截图，不纳入版本控制。
