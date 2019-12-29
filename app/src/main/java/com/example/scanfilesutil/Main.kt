package com.example.scanfilesutil

fun main() {
    val root: String = "/storage/emulated/0/"
    val now1 = "/storage/emulated/0/update.zip/abc"

    var level: Int = 0
    now1.replace(root.trimEnd { it == '/' }, "").map {
        if (it == '/') {
            level++
            println("$level 级目录")
        }
    }
    println()
}