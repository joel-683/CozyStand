# CozyStand

Native Android bedside clock project built with Kotlin and Jetpack Compose.

## Build APK with GitHub Actions

1. Upload the **contents** of this folder to the root of a GitHub repository on the `main` branch.
2. Open the repository's **Actions** tab.
3. Select **Build CozyStand APK** and choose **Run workflow** (or let the push trigger it).
4. When the run succeeds, download the `CozyStand-debug-apk` artifact.
5. Extract the artifact ZIP to obtain `app-debug.apk`.

The workflow uses the Android SDK available on GitHub-hosted Ubuntu runners and does not use `android-actions/setup-android`.

## Local build

Open this folder in Android Studio and allow Gradle sync. The project currently uses Android Gradle Plugin 8.10.1, Kotlin 2.1.21, Java 17 and compile SDK 35.

## Build APK on GitHub Actions

The repository includes `.github/workflows/build-apk.yml`. Upload the contents of this folder to the root of a GitHub repository, including the hidden `.github` folder. Then open **Actions**, select **Build CozyStand APK**, and run the workflow. When it succeeds, download the `CozyStand-debug-apk` artifact from the workflow run.
