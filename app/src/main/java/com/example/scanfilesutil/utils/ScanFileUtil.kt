package com.example.scanfilesutil.utils

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FilenameFilter
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 扫描文件工具
 */
class ScanFileUtil {


    companion object {
        val externalStorageDirectory by lazy {
            Environment.getExternalStorageDirectory().absolutePath
        }
        val android_app_data_folder by lazy {
            "${externalStorageDirectory}/Android/data/"
        }

    }

    /**
     * 是否停止扫描
     */
    private var isStop = true


    /**
     * 要扫描的根路径
     */
    private val mRootPath: String
    /**
     * 规律规则
     */
    private var mFilter: FilenameFilter? = null
    /**
     * 协程列队
     */
    private val mQueue: ConcurrentLinkedQueue<Deferred<Boolean>>
    /**
     * 扫描完成回调
     */
    private var mCompleteCallBack: (() -> Unit)? = null


    constructor(rootPath: String) {
        this.mRootPath = rootPath
        this.mQueue = ConcurrentLinkedQueue<Deferred<Boolean>>()
    }

    constructor(rootPath: String, complete: () -> Unit) {
        this.mRootPath = rootPath
        this.mQueue = ConcurrentLinkedQueue<Deferred<Boolean>>()
        mCompleteCallBack = complete
    }

    /**
     * 设置扫结束回调
     */
    fun completeCallBack(success: () -> Unit) {
        mCompleteCallBack = success
    }

    /**
     * 开始异步扫描文件
     */
    fun startAsyncScan(callback: (file: File) -> Unit) {
        if (!isStop) {
            Log.d("Scan", "已经在扫描了")
            return
        }
        isStop = false
        val file = File(mRootPath)
        if (!file.exists()) {
            return
        }
        //开始扫描
        asyncScan(file, callback)
        //检查协程列队
        checkQueue()
    }

    fun stop() {
        isStop = true
        mQueue.clear()
    }

    /**
     * 异步扫描文件 递归调用
     * @param dirOrFile 要扫描的文件 或 文件夹
     * @param callback 文件回调 再子线程中 不可操作UI 将扫描到的文件通过callback调用
     */
    private fun asyncScan(dirOrFile: File, callback: (file: File) -> Unit) {
        if (isStop) {
            //需要停止
            return
        }
        //将任务添加到列队中
        mQueue.offer(GlobalScope.async {
            if (dirOrFile.isFile) {
                if (filterFile(dirOrFile)) {
                    callback(dirOrFile)
                }
                return@async true
            }

            if (dirOrFile.isDirectory) {
                if (filterFile(dirOrFile)) {
                    callback(dirOrFile)
                }
            }

            val rootFile = getFilterFilesList(dirOrFile)
            //遍历
            rootFile?.map {
                if (isStop) {
                    //需要停止
                    async@ cancel()
                    return@map
                }
                //如果是文件夹
                if (it.isDirectory) {
                    if (filterFile(it)) {
                        callback(it)
                    }
                    //再次调用自己
                    asyncScan(it, callback)
                } else {
                    //验证过滤规则
                    if (filterFile(it)) {
                        callback(it)
                    }
                }
            }
            true
        })
    }

    /**
     * 校验过滤文件
     */
    private fun filterFile(file: File): Boolean {
        return if (mFilter == null) {
            !isStop
        } else {
            mFilter!!.accept(file, file.name) && !isStop
        }
    }

    /**
     * 检查协程列队 当所有列队都已完成执行successCallback
     */
    private fun checkQueue() {
        GlobalScope.launch(Dispatchers.IO) {
            //当列队为空的时候就扫描完成了
            while (true) {
                if (isStop) {
                    //需要停止
                    break
                }
                //获取头队伍等待await返回完成
                if (mQueue.poll()?.await() != true) {
                    if (isStop) {
                        //需要停止
                        break
                    }
                    if (mCompleteCallBack != null) {
                        GlobalScope.launch(Dispatchers.Main) {
                            mCompleteCallBack?.invoke()
                            isStop = true
                        }
                    }
                    break
                }
            }
        }
    }

    /**
     * 设置文件扫描过滤规则
     */
    fun setFilter(filter: FilenameFilter?) {
        this.mFilter = filter
    }

    /**
     * 获取文件夹中的文件列表 并且引用过滤规则
     */
    private fun getFilterFilesList(file: File): Array<File>? {
        return file.listFiles()
    }


    /**
     * 过滤器构造器
     */
    class FileFilterBuilder {

        val filseFilterSet: MutableSet<String> = hashSetOf()

        /**
         * 隐藏文件 true扫描 false不扫描
         */
        private var isHiddenFiles = false

        /**
         * 只要扫描文件
         */
        private var isOnlyFile = false

        /**
         * 只扫描文件夹
         */
        private var isOnlyDir = false

        fun onlyScanDir() {
            isOnlyDir = true
        }

        fun onlyScanFile() {
            isOnlyFile = true
        }

        /**
         * text文件 true扫描txt文件 false不扫描txt文件
         */
        fun scanTxTFiles() {
            filseFilterSet.add("txt")
        }

        fun scanHiddenFiles() {
            isHiddenFiles = true
        }

        /**
         *  apk文件 true扫描Apk文件 false不扫描Apk文件
         */
        fun scanApkFiles() {
            filseFilterSet.add("apk")
        }

        /**
         *  temp文件 true扫描 false不扫描
         */
        fun scanTempFiles() {
            filseFilterSet.add("temp")
        }

        /**
         * log文件 true扫描 false不扫描
         */
        fun scanLogFiles() {
            filseFilterSet.add("log")
            filseFilterSet.add(".qlog")
        }

        /**
         * 扫描文档类型文件
         */
        fun scanDocumentFiles() {
            filseFilterSet.add("txt")
            filseFilterSet.add("pdf")
            filseFilterSet.add("doc")
            filseFilterSet.add("docx")
            filseFilterSet.add("xls")
            filseFilterSet.add("xlsx")
        }

        /**
         * 扫描图片类型文件
         */
        fun scanPictureFiles() {
            filseFilterSet.add("jpg")
            filseFilterSet.add("jpeg")
            filseFilterSet.add("png")
            filseFilterSet.add("bmp")
            filseFilterSet.add("gif")
        }

        /**
         * 扫描多媒体文件类型
         */
        fun scanVideoFiles() {
            filseFilterSet.add("mp4")
            filseFilterSet.add("avi")
            filseFilterSet.add("wmv")
            filseFilterSet.add("flv")
        }

        /**
         * 扫描音频文件类型
         */
        fun scanMusicFiles() {
            filseFilterSet.add("mp3")
            filseFilterSet.add("ogg")
        }

        /**
         * 扫描压缩包文件类型
         */
        fun scanZipFiles() {
            filseFilterSet.add("zip")
            filseFilterSet.add("rar")
            filseFilterSet.add("7z")
        }


        fun build(): FilenameFilter {
            return object : FilenameFilter {
                override fun accept(dir: File, name: String): Boolean {
                    if (isHiddenFiles && dir.isHidden) {
                        return true
                    }

                    if (isOnlyDir) {
                        return dir.isDirectory
                    }

                    if (isOnlyFile) {
                        if (dir.isFile) {
                            if (filseFilterSet.isNotEmpty()) {
                                //获取文件后缀
                                val suffix: String =
                                    name.substring(name.indexOfLast { it == '.' } + 1, name.length)
                                        .toLowerCase()
                                return filseFilterSet.contains(suffix)
                            }
                            return true
                        } else {
                            return false
                        }
                    }

                    if (filseFilterSet.isEmpty()) {
                        return true
                    }

                    //获取文件后缀
                    val suffix: String =
                        name.substring(name.indexOfLast { it == '.' } + 1, name.length)
                            .toLowerCase()
                    return filseFilterSet.contains(suffix)
                }
            }

        }

    }

}