// 仓库统一走阿里云镜像（本构建环境无法直连 dl.google.com / repo1.maven.org）
pluginManagement {
    repositories {
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
