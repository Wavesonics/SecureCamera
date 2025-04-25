plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlin.serialization)
}

android {
	namespace = "com.darkrockstudios.app.securecamera"
	compileSdk = libs.versions.compileSdk.get().toInt()

	packaging {
		resources {
			excludes += "META-INF/LICENSE.md"
			excludes += "META-INF/LICENSE-notice.md"
		}
	}

	defaultConfig {
		applicationId = "com.darkrockstudios.app.securecamera"
		minSdk = libs.versions.minSdk.get().toInt()
		targetSdk = libs.versions.targetSdk.get().toInt()
		versionCode = libs.versions.versionCode.get().toInt()
		versionName = libs.versions.versionName.get()

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	signingConfigs {
		create("release") {
			storeFile = file(System.getenv("KEYSTORE_FILE") ?: "keystore.jks")
			storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
			keyAlias = System.getenv("KEY_ALIAS") ?: ""
			keyPassword = System.getenv("KEY_PASSWORD") ?: ""
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			signingConfig = signingConfigs.getByName("release")
		}
		debug {
			applicationIdSuffix = ".debug"
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
		targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
	}
	kotlinOptions {
		jvmTarget = libs.versions.javaVersion.get()
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
	dependenciesInfo {
		includeInApk = false
		includeInBundle = false
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.timber)
	implementation(libs.camerak)
	implementation(libs.camerak.image.saver.plugin)
	implementation(libs.camerak.qr.scanner.plugin)
	implementation(libs.kim)
	implementation(project.dependencies.platform(libs.koin.bom))
	implementation(libs.koin.core)
	implementation(libs.koin.android)
	implementation(libs.koin.androidx.compose)
	implementation(libs.koin.androidx.compose.navigation)
	implementation(libs.koin.core.coroutines)
	implementation(libs.androidx.material.icons.extended)
	implementation(platform(libs.cryptography.bom))
	implementation(libs.cryptography.core)
	implementation(libs.cryptography.provider.jdk)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.datastore.preferences.core)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.accompanist.permissions)
	implementation(libs.androidx.lifecycle.runtime.compose)

	testImplementation(libs.junit)
	testImplementation(libs.koin.test.junit4)
	testImplementation(libs.koin.android.test)
	testImplementation(libs.mockk)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	androidTestImplementation(libs.mockk.android)
	androidTestImplementation(libs.mockk.agent)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
	testImplementation(kotlin("test"))
}
