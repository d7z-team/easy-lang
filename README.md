# Kotlin 模板

> 一套简单的 Kotlin/JVM 多模块项目模板 ,使用 Gradle.

## 特性

- 跟随`Gradle` 和 `Kotlin` 最新正式版
- 支持 `Java Module`
- 支持 `Java 11 LTS`
- 支持 `Maven BOM`
- 支持统一化版本管理

## 快速开始

1. 使用此模板创建项目
2. 修改[LICENSE](./LICENSE) 为合适的分发协议
3. 重建 [README.md](./README.md) 文件或使用[README.template](./README.template.md)模板
4. 修改 [settings.gradle.kts](./settings.gradle.kts) 中 `project.name` 为合适的名称
5. 更新 [version.properties](./version.properties) 内的版本
6. 在项目根目录下执行 `./gradlew build -x test`
7. 在项目根目录下执行 `./gradlew test`
8. 删除 [template](./template) 下的模板代码
9. 修改 [module-info.java](./template/src/main/java/module-info.java) 中的配置

## 可选操作

### 开启 Github Actions

**注意**：Github Actions 服务由 Github 公司提供，与此项目无关

地址：[GitHub Actions](https://docs.github.com/cn/actions)

Github Action 默认处于禁用状态，开启方法如下：

1. 编辑文件 [test-action.yml](./.github/workflows/test-action.yml)
2. 去除 `on` 及其子节点的所有 `#` 注释
3. 提交推送更改

如果你不需要的话删除 [workflows](./.github/workflows) 文件夹即可。

### 开启 Maven BOM

Maven BOM 默认开启且开箱即用，如需关闭删除 [bom](./bom) 文件夹即可，同时在 [settings.gradle.kts](./settings.gradle.kts) 下移除 `include("bom")` 。

## 使用建议

- 统一化版本管理，将依赖版本配置到 `version.properties` 下
- IDE 安装 `CheckStyle` 工具，统一代码格式

## 更新日志

注意：从 `20220508` 版本开始不再发布 `release` ，直接克隆分支即可

- **20220508**
  - 新增统一化版本管理
  - 去除 `Github Action` 自动执行
  - 迁移 `Maven` 推送地址至 `buildSrc`

- **20220403**
  - 更新 `Gradle` 版本至 `7.4.2`
  - 增加 [Github Action](https://github.com/features/actions)
  - 增加统一化版本管理

- **20220111**
  - 更新 `Kolin` 版本至 `1.6.10`
  - 更新 `Gradle` 版本至 `7.3.3`
  - 更新 `Junit 5` 版本至 `5.8.2`
  - 添加 `ktlint` 用于 CheckStyle
  - 添加 `application` 模块

- **20210720**
  - 更新 `Kolin` 版本至 `1.5.21`
  - 更新 `Gradle` 版本至 `7.1.1`
  - 去除第三方不可信 `Maven` 源以降低 [供应链攻击](https://en.wikipedia.org/wiki/Supply_chain_attack) 风险
  - 重命名默认子模块名称

- **20210322**
  - 更新 `Kolin` 版本至 `1.4.31`
  - 更新 `Gradle` 版本至 `6.8.3`
  - 去除 `resources` 下孤立文件
  - 将插件版本配置合并到 [gradle.properties](./gradle.properties) 下

- **20201208**
  - 新增 Java 9 模块化支持
  - 修复在使用`maven-publish`打包时未对源码打包的问题

- **20201125**
  - 更改项目名称为 `GradleKotlinTemplate`
  - 添加 `maven-publish` 插件，现在可以使用 `gradlew publishToMavenLocal` 了
  - 修改 `Kotlin` 插件编译生成目录 [build.gradle.kts](./template/build.gradle.kts)

- **20201120**
  - 更新 `kotlin` 为 `1.4.10`
  - 更新 `gradle` 为 `6.5.1`
  - 删除过时的 `buildSrc` 方案

## LICENSE

请看 [LICENSE](./LICENSE) 来了解此模板的 LICENSE
