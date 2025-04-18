# Project Guidelines

We're creating an Android app that is a privacy focused camera.

We want to take pictures, strip all metadata, store them in app-private encrypted storage
and require a PIN separate from the device's PIN to view them.

With all of that privacy in mind, we do want to be able to use the normal Android "share"
system for getting the images out of our secure storage and into other apps.

As a stretch goal, I want to provide further security features such as automatic face
blurring.

* The project consists of a single `app` module which is the whole application
* We're using Kotlin
* Prefer KMP libraries over Kotlin/JVM when possible
* We're using Compose for the UI
* We use tabs, no spaces
* We should write unit tests when possible for new functions or features
* We use Mockk for our test mocking
* Comment code only sparingly. Obvious comments should be left out, only comment on code that is slight weird or complex