apply(from = "repositories.gradle.kts")

include(":core")

loadAllIndividualExtensions()

fun loadAllIndividualExtensions() {
    File(rootDir, "aniyomi/src").eachDir { dir ->
        dir.eachDir { subdir ->
            include("aniyomi:src:${dir.name}:${subdir.name}")
        }
    }
}
fun loadIndividualExtension(lang: String, name: String) {
    include("aniyomi:src:$lang:$name")
}

fun File.eachDir(block: (File) -> Unit) {
    val files = listFiles() ?: return
    for (file in files) {
        if (file.isDirectory && file.name != ".gradle" && file.name != "build") {
            block(file)
        }
    }
}
