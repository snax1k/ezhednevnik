plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Ключи экранов для Navigation 3 помечаются @Serializable, чтобы стек переходов переживал поворот экрана
    alias(libs.plugins.kotlin.serialization)
    // KSP генерирует код Room; kapt со встроенным Kotlin в AGP 9 не работает
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.snax1k.chekki"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.snax1k.chekki"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        // 0.1 — первая рабочая версия на телефоне, ещё не выпуск
        versionName = "0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Ключ подписи хранится вне проекта, путь и пароли — в личном ~/.gradle/gradle.properties:
    // CHEKKI_STORE_FILE, CHEKKI_STORE_PASSWORD, CHEKKI_KEY_ALIAS, CHEKKI_KEY_PASSWORD.
    // Без них выпускная сборка собирается неподписанной.
    val releaseStoreFile = providers.gradleProperty("CHEKKI_STORE_FILE").orNull
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("CHEKKI_STORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("CHEKKI_KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("CHEKKI_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        debug {
            // Отладочная сборка ставится рядом с настоящей и не трогает её данные
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            // R8 сжимает и оптимизирует всё приложение. packageScope из шаблона убран: это экспериментальный режим
            // постепенного включения, без флага android.r8.gradual.support он ломал запуск (IllegalAccessError в kotlin.collections)
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

room {
    // Схема базы по версиям: нужна, чтобы писать и проверять миграции
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // Проверка миграций базы: MigrationTestHelper открывает базу по сохранённой схеме старой версии
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}