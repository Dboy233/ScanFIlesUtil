package com.example.scanfilesutil.utils

import android.os.Environment
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
     * 扫描到文件回调规则
     */
    private var mCallBackFilter: FilenameFilter? = null
    /**
     * 扫描时的过滤规则
     */
    private var mScanFilter: FilenameFilter? = null
    /**
     * 协程列队
     */
    private val mQueue: ConcurrentLinkedQueue<Deferred<Boolean>>
    /**
     * 扫描完成回调
     */
    private var mCompleteCallBack: (() -> Unit)? = null

    /**
     * 扫描层级
     */
    private var mScanLevel = -1L
    /**
     * 记录扫描层级
     */
    private var mScanLevelCont = -1L

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
     * 设置扫描层级数
     */
    fun setScanLevel(level: Long) {
        mScanLevel = level
    }

    /**
     * 停止
     */
    fun stop() {
        isStop = true
        mQueue.clear()
    }


    /**
     * 开始异步扫描文件
     */
    fun startAsyncScan(callback: (file: File) -> Unit) {
        if (!isStop) {
            return
        }
        mScanLevelCont = -1
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

    /**
     * 异步扫描文件 递归调用
     * @param dirOrFile 要扫描的文件 或 文件夹
     * @param callback 文件回调 再子线程中 不可操作UI 将扫描到的文件通过callback调用
     */
    private fun asyncScan(dirOrFile: File, callback: (file: File) -> Unit) {
        mScanLevelCont++
        if (mScanLevel > 0) {
            if (mScanLevelCont >= mScanLevel) {
                return
            }
        }
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
        return if (mCallBackFilter == null) {
            !isStop
        } else {
            mCallBackFilter!!.accept(file, file.name) && !isStop
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
     *  文件通过callback返回结果时过滤规则
     */
    fun setCallBackFilter(filter: FilenameFilter?) {
        this.mCallBackFilter = filter
    }

    /**
     * 扫描时过滤规则
     */
    fun setScanFilter(filter: FilenameFilter?) {
        this.mScanFilter = filter
    }


    /**
     * 获取文件夹中的文件列表 并且引用过滤规则
     */
    private fun getFilterFilesList(file: File): Array<File>? {
        return if (mScanFilter == null) {
            file.listFiles()
        } else {
            file.listFiles(mScanFilter)
        }
    }


    /**
     * 过滤器构造器
     */
    class FileFilterBuilder {

        val mFilseFilterSet: MutableSet<String> = hashSetOf()

        val mNameLikeFilterSet: MutableSet<String> = hashSetOf()

        val mNameNotLikeFilterSet: MutableSet<String> = hashSetOf()

        private var isHiddenFiles = false

        private var isOnlyFile = false

        private var isOnlyDir = false
        /**
         * 只扫描文件夹
         */
        fun onlyScanDir() {
            isOnlyDir = true
        }

        /**
         * 只要扫描文件
         */
        fun onlyScanFile() {
            isOnlyFile = true
        }

        /**
         * 扫描名字像它的
         */
        fun scanNameLikeIt(like: String) {
            mNameLikeFilterSet.add(like.toLowerCase())
        }

        /**
         * 扫描名字不像它的文件
         * 也就是不扫描名字像这个的文件
         */
        fun scanNameNotLikeIt(like: String) {
            mNameNotLikeFilterSet.add(like.toLowerCase())
        }

        /**
         * text文件 true扫描txt文件 false不扫描txt文件
         */
        fun scanTxTFiles() {
            mFilseFilterSet.add("txt")
        }

        /**
         * 隐藏文件 true扫描 false不扫描
         */
        fun scanHiddenFiles() {
            isHiddenFiles = true
        }

        /**
         *  apk文件 true扫描Apk文件 false不扫描Apk文件
         */
        fun scanApkFiles() {
            mFilseFilterSet.add("apk")
        }

        /**
         *  temp文件 true扫描 false不扫描
         */
        fun scanTempFiles() {
            mFilseFilterSet.add("temp")
        }

        /**
         * log文件 true扫描 false不扫描
         */
        fun scanLogFiles() {
            mFilseFilterSet.add("log")
            mFilseFilterSet.add(".qlog")
        }

        /**
         * 扫描文档类型文件
         */
        fun scanDocumentFiles() {
            mFilseFilterSet.add("txt")
            mFilseFilterSet.add("pdf")
            mFilseFilterSet.add("doc")
            mFilseFilterSet.add("docx")
            mFilseFilterSet.add("xls")
            mFilseFilterSet.add("xlsx")
        }

        /**
         * 扫描图片类型文件
         */
        fun scanPictureFiles() {
            mFilseFilterSet.add("jpg")
            mFilseFilterSet.add("jpeg")
            mFilseFilterSet.add("png")
            mFilseFilterSet.add("bmp")
            mFilseFilterSet.add("gif")
        }

        /**
         * 扫描多媒体文件类型
         */
        fun scanVideoFiles() {
            mFilseFilterSet.add("mp4")
            mFilseFilterSet.add("avi")
            mFilseFilterSet.add("wmv")
            mFilseFilterSet.add("flv")
        }

        /**
         * 扫描音频文件类型
         */
        fun scanMusicFiles() {
            mFilseFilterSet.add("mp3")
            mFilseFilterSet.add("ogg")
        }

        /**
         * 扫描压缩包文件类型
         */
        fun scanZipFiles() {
            mFilseFilterSet.add("zip")
            mFilseFilterSet.add("rar")
            mFilseFilterSet.add("7z")
        }

        /**
         * 检查名字相似过滤
         */
        private fun checkNameLikeFilter(name: String): Boolean {
            //相似名字获取过滤
            if (mNameLikeFilterSet.isNotEmpty()) {
                mNameLikeFilterSet.map {
                    if (name.toLowerCase().contains(it)) {
                        return true
                    }
                }
                return false
            }
            return true
        }

        /**
         * 检查名字不相似过滤
         */
        private fun checkNameNotLikeFilter(name: String): Boolean {
            //名字不相似顾虑
            if (mNameNotLikeFilterSet.isNotEmpty()) {
                mNameNotLikeFilterSet.map {
                    if (name.toLowerCase().contains(it)) {
                        return false
                    }
                }
                return true
            }
            return true
        }

        fun build(): FilenameFilter {
            return object : FilenameFilter {
                override fun accept(dir: File, name: String): Boolean {
                    //隐藏文件扫描规则
                    if (isHiddenFiles && dir.isHidden) {
                        return true
                    }

                    //只扫描文件夹
                    if (isOnlyDir) {
                        return dir.isDirectory
                                && checkNameLikeFilter(name)
                                && checkNameNotLikeFilter(name)
                    }
                    //只扫描文件 同时应用文件扫描规则
                    if (isOnlyFile) {
                        if (dir.isFile) {
                            if (mFilseFilterSet.isNotEmpty()) {
                                //获取文件后缀
                                val suffix: String =
                                    name.substring(name.indexOfLast { it == '.' } + 1, name.length)
                                        .toLowerCase()
                                return mFilseFilterSet.contains(suffix) && checkNameLikeFilter(name)
                                        && checkNameNotLikeFilter(name)
                            }
                            return checkNameLikeFilter(name)
                                    && checkNameNotLikeFilter(name)
                        } else {
                            return false
                        }
                    }
                    //文件扫描规则
                    if (mFilseFilterSet.isEmpty()) {
                        return checkNameLikeFilter(name)
                                && checkNameNotLikeFilter(name)
                    }
                    //获取文件后缀
                    val suffix: String =
                        name.substring(name.indexOfLast { it == '.' } + 1, name.length)
                            .toLowerCase()
                    return mFilseFilterSet.contains(suffix)
                            && checkNameLikeFilter(name)
                            && checkNameNotLikeFilter(name)
                }
            }

        }

    }

}