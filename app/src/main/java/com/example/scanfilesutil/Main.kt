package com.example.scanfilesutil

fun main() {
    val boolList = mutableListOf<Boolean>()
    boolList.add(true)
    boolList.add(false)
    println((boolList.isEmpty() || !boolList.contains(false)))
}