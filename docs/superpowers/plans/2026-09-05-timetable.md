# 拾课 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox syntax for tracking.

**Goal:** 交付可安装的个人课程表 APK，包含文件和教务导入、课程管理及小组件。

**Architecture:** 纯 Java core 模型与解析器，安卓界面、文件读取和本地存储作为边界。所有来源统一返回 ImportResult，经预览校对后保存。

**Tech Stack:** Java17、Android API36、Gradle、JUnit4、JXL、Jsoup、PDFBox Android、中文 ML Kit。

**Spec:** ../specs/2026-09-05-timetable-design.md

## Global Constraints
- UTF-8、中文注释；minSdk26、targetSdk36。
- 教师选填，单周/双周必须精确展开；不确定的安排进入 warnings。
- 手机本地解析和保存；网络只用于用户启动的教务登录。

## Task 1: 排课核心及构建
Files: core/src/main/java/cn/edu/csu/timetable/core/{Course,Schedule,ImportResult}.java; core/src/test/java/cn/edu/csu/timetable/core/ScheduleTest.java; 根 Gradle 与 scripts/build.ps1。
Interfaces: Course(name,teacher,location,day,start,end,weeks), Schedule.weekOf(LocalDate,LocalDate), Schedule.onDate(List<Course>,LocalDate,LocalDate)。
- [x] 先执行测试：`assertEquals(1, Schedule.weekOf(LocalDate.parse("2026-09-06"), LocalDate.parse("2026-09-06")))`；周六仍为1，周日变2；单周跳过偶数周。
- [x] 实现周计算：`ChronoUnit.DAYS.between(start, date)` 向下整除7再加1，开学前不返回第1周。
- [x] 运行 `:core:test`；准备 Android SDK 与构建脚本。

## Task 2: 导入解析
Files: core/.../importer/*, core/src/test/.../ImporterTest.java; app/.../DocumentReader.java。
Interfaces: ImportResult.courses/warnings/termName/startDate/times; TableImporter.parse(List<List<String>>), TextImporter.parse(String)。
- [x] 对真实 XLS 做只读集成测试，核对周一第一节两门课与周四单周课。
- [x] 表格识别行列方向与合并单元格，解析课程块与作息；HTML与DOCX表格复用规范化表格。
- [x] PDF文字和OCR提取后解析，缺失星期/节次明确进入待校对，不静默填充。
- [x] 运行解析单元测试与真实样本检查。

## Task 3: 界面与持久化
Files: app/.../{MainActivity,CourseStore,WeekView,Ui}.java。
- [x] 周/今日导航、课程详情、编辑和删除、手动添加、日期设置与主题。
- [x] 导入界面展示每条课程，允许编辑/排除，并提供替换/合并的明确选择。
- [x] 存储用私有 SharedPreferences JSON，每次成功保存后刷新小组件。
- [x] 构建并在可用设备验证空状态、导入预览和保存重启。

## Task 4: 教务与小组件
Files: app/.../{PortalActivity,TodayWidget,NextWidget,WidgetUpdater}.java; app/src/main/res/xml/*。
- [x] WebView 用户登录，限定中南大学来源，使用 evaluateJavascript 读取课程表DOM，无密码桥接。
- [x] 返回 ImportResult 到预览页，读取失败给出具体校对入口。
- [x] RemoteViews 当前与下一节课程，时间/日期/时区/数据变化后更新，点击打开App。
- [x] 检验已结束课程、无课日、未配置时刻的显示。

## Task 5: 交付验证
Files: README.md, docs/verification.md, outputs/*.apk。
- [x] `:core:test :app:assembleDebug :app:lintDebug`，签名与安装验证。
- [x] 独立代码复核、修复影响导入正确性和数据保存的发现。
- [x] 记录设备验证限制、安装方法、源文件格式边界；交付APK。
