// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    `maven-publish`
}

//subprojects {
//    /**
//     * Setup maven-publish on all modules; see each module for publication details.
//     */
//    apply(plugin = "maven-publish")
//}