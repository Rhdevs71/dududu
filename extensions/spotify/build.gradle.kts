android {
    namespace = "app.morphe.extension.spotify"

    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    compileOnly(project(":extensions:shared:library"))
    compileOnly(libs.morphe.extensions.library)
    compileOnly(libs.annotation)
    compileOnly(libs.appcompat)
}
