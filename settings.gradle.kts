// 仓库统一走阿里云镜像（本构建环境无法直连 dl.google.com / repo1.maven.org）
pluginManagement {
    repositories {
        // Gradle Plugin Portal 必须排在阿里云前面：KSP 的插件标记不在 Google Maven，
        // 而阿里云的 google 镜像对缺失构件返回 502（不是 404），Gradle 会当作仓库异常直接判插件不存在。
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云 central 对少数构件（如 KSP 插件标记）会返回 502，导致整个仓库被 Gradle 禁用；
        // 官方 Maven Central 作为优先的 central 源，google 构件仍走上方阿里云镜像。
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}

rootProject.name = "ScoreTrace"
include(":app")
