Purple Robot
====================

Purple Robot is an Android application for creating automated experiences on the Android platform with sensing and inference capabilities. The app was created by [the Center for Behavioral Intervention Technologies](http://cbits.northwestern.edu) at Northwestern University as an infrastructural component for a variety of behavioral and medical intervention applications.


## Building Purple Robot

Purple Robot is a large application that includes a variety of service integration libraries as well as full language runtimes for both the JavaScript and Scheme programming languages. Consequently, it cannot be built by traditional Android IDEs given its reliance on [ProGuard](http://proguard.sourceforge.net/) to shrink installation below [the 65k methods](https://code.google.com/p/android/issues/detail?id=58008) limitation imposed by Android. The instructions below describe how to create an APK Android package for your own testing and development.

### Prerequisites

Purple Robot depends upon the following packages for compilation and installation:

* [Git](http://git-scm.com/): This may already be installed on your system.
* [Android SDK](http://developer.android.com/sdk/index.html): SDK installations that accompany Android IDEs like Eclipse and Android Studio may be used to build Purple Robot.
* [Gradle](http://www.gradle.org/): Install version 1.12, not 2.0.
* [Java](http://www.oracle.com/technetwork/java/index.html): This may already be installed.

After installing these prerequisites, open the Android SDK Manager and verify that the following packages are installed:

* Android 4.4.2 (API 19) SDK Platform
* Android Support Repository
* Google Repository
* Google USB Driver (or suitable ADB driver for your device)

### Pull from GitHub

Once the prerequisites are installed, clone the Purple Robot Git repository at

    https://github.com/cbitstech/Purple-Robot.git
  
This will check out the Purple Robot app as well as some supporting technologies. Once the repository has been fetched, initialize and update the submodules from within the `Purple-Robot` folder:

    git submodule init
    git submodule update

These commands will retrieve [the Facebook SDK](https://developers.facebook.com/docs/android) and [Anthracite](https://github.com/cbitstech/anthracite-clients-android) logging dependencies.

### Building Purple Robot

Next, go to the `Purple Robot` folder and rename the following files:

* `gradle.properties.template` to `gradle.properties`
* `local.properties.template` to `local.properties.template`
    
In `local.properties`, set the location of your Android SDK. In `gradle.properties`, set the values for your Android signing keystore and key. Refer to [Google's signing documentation](http://developer.android.com/tools/publishing/app-signing.html) for more details.

To test your installation, within the `Purple Robot` folder, run

     gradle clean
    
If that command completes successfully, you can build and install Purple Robot onto an attached Android phone by running

     gradle installRelease
    
If you receive an error about the Java heap size, you may need to set an environmental variable instructing Gradle to use more memory:

    export GRADLE_OPTS=-Xmx2048m
    
On non-Unix platforms, you may also need to set your `JAVA_HOME` environment variable to point to you local copy of the Java Development Kit.

### Questions or comments?

If you have any questions or comments, please send an e-mail to [Chris Karr](mailto:c-karr@northwestern.edu) and I'll get back to you as soon as I can.
