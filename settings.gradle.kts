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
        // 阿里云 central 优先：repo1.maven.org 会把部分构件（如 kotlin-compiler-embeddable）
        // 302 到 GitHub Releases，CI 上那条链路会 504。阿里云直出 jar，不做跳转。
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://repo1.maven.org/maven2") }
    }
}

rootProject.name = "ScoreTrace"
include(":app")
