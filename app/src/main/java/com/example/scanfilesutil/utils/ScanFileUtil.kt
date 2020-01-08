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
        //手机外部存储根目录
        val externalStorageDirectory by lazy {
            Environment.getExternalStorageDirectory().absolutePath
        }
        //手机app缓存存放路径
        val android_app_data_folder by lazy {
            "$externalStorageDirectory/Android/data"
        }

        /**
         * 等待 多个任务列队完成
         */
        fun await(vararg deferred: Deferred<Boolean>?, complete: () -> Unit) {
            GlobalScope.async(Dispatchers.IO) {
                //检查所有任务是否在运行 在运行等待运行结束
                deferred.map {
                    if (it?.isActive == true) {
                        it.await()
                    }
                }
                //所有任务都结束了在main线程 回调完成函数
                withContext(Dispatchers.Main) {
                    complete()
                }
            }
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
     * 扫描到文件回调过滤规则
     */
    private var mCallBackFilter: FilenameFilter? = null
    /**
     * 扫描时的过滤规则 只建议用来过滤隐藏文件和大小为0的文件
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


    constructor(rootPath: String) {
        this.mRootPath = rootPath.trimEnd { it == '/' }
        this.mQueue = ConcurrentLinkedQueue<Deferred<Boolean>>()
    }

    constructor(rootPath: String, complete: () -> Unit) {
        this.mRootPath = rootPath.trimEnd { it == '/' }
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
    }


    /**
     * 开始异步扫描文件
     */
    fun startAsyncScan(callback: (file: File) -> Unit) {
        if (!isStop) {
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
            //扫描路径层级判断
            if (checkLevel(dirOrFile)) {
                return@async true
            }

            //检查是否是文件 是文件就直接回调 返回true
            if (dirOrFile.isFile) {
                if (filterFile(dirOrFile)) {
                    callback(dirOrFile)
                }
                return@async true
            }
            //获取文件夹中的文件集合
            val rootFile = getFilterFilesList(dirOrFile)
            //遍历文件夹
            rootFile?.map {
                //是否需要停止
                if (isStop) {
                    return@async true
                }
                //如果是文件夹 回调 递归调用函数 再遍历判断
                if (it.isDirectory) {
                    if (filterFile(it)) {
                        callback(it)
                    }
                    //再次调用自己
                    asyncScan(it, callback)
                }
                //是文件 回调
                else {
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
     * 检查扫描路径是否已经到指定的层级
     */
    private fun checkLevel(dirOrFile: File): Boolean {
        if (mScanLevel != -1L) {
            var scanLevelCont = 0L
            dirOrFile.absolutePath.replace(mRootPath, "").map {
                if (it == '/') {
                    scanLevelCont++
                    if (scanLevelCont >= mScanLevel) {
                        return true
                    }
                }
            }
        }
        return false
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
     * 扫描监视回调
     */
    private var mScanningCheckQueue: Deferred<Boolean>? = null

    /**
     * 等待完成 在协程中执行
     */
    suspend fun awaitComplete(): Boolean {
        return mScanningCheckQueue?.await() == true
    }

    /**
     * 获取扫描任务完成回调
     * 使用ScanFileUtil.await()时调用此方法
     */
    fun getAwaitComplete(): Deferred<Boolean>? {
        return mScanningCheckQueue
    }

    /**
     * 检查协程列队 当所有列队都已完成执行successCallback
     */
    private fun checkQueue() {
        mScanningCheckQueue = GlobalScope.async<Boolean>(Dispatchers.IO) {
            //当列队为空的时候就扫描完成了
            while (true) {
                if (isStop) {
                    //需要停止
                    return@async true
                }
                //获取头队伍等待await返回完成
                if (mQueue.poll()?.await() != true) {
                    if (isStop) {
                        //需要停止
                        return@async true
                    }
                    if (mCompleteCallBack != null) {
                        withContext(Dispatchers.Main) {
                            mCompleteCallBack?.invoke()
                            isStop = true
                        }
                    }
                    return@async true
                }
            }
            return@async true
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
        /**
         * 添加自定义filter规则 集合
         */
        val customFilterList: MutableList<FilenameFilter> = mutableListOf<FilenameFilter>()
        /**
         * 文件类型&文件后缀 扫描过滤规则 集合
         */
        val mFilseFilterSet: MutableSet<String> = hashSetOf()
        /**
         * 扫描名字像它的 集合
         */
        val mNameLikeFilterSet: MutableSet<String> = hashSetOf()
        /**
         * 扫描名字不像它的文件 集合
         * 也就是不扫描名字像这个的文件 集合
         */
        val mNameNotLikeFilterSet: MutableSet<String> = hashSetOf()
        /**
         * 是否扫描隐藏文件 true扫描 false不扫描
         */
        private var isScanHiddenFiles = true
        /**
         * 只要扫描文件
         */
        private var isOnlyFile = false
        /**
         * 只扫描文件夹
         */
        private var isOnlyDir = false


        /**
         * 添加自定义filter规则
         * Add custom filter rule
         */
        fun addCustomFilter(filter: FilenameFilter) {
            customFilterList.add(filter)
        }

        /**
         * 只扫描文件夹
         * Scan folders only
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
         * 扫描名字像它的文件或者文件夹
         * Scan names like its files or folders
         */
        fun scanNameLikeIt(like: String) {
            mNameLikeFilterSet.add(like.toLowerCase())
        }

        /**
         * 扫描名与其文件不同
         * 也就是说，不要扫描这样的文件
         * Scan name is not like its file
         * That is, don't scan files with names like this
         */
        fun scanNameNotLikeIt(like: String) {
            mNameNotLikeFilterSet.add(like.toLowerCase())
        }

        /**
         * 扫描TxT文件
         */
        fun scanTxTFiles() {
            mFilseFilterSet.add("txt")
        }

        /**
         * 不扫描隐藏文件
         * Don't scan hidden files
         */
        fun notScanHiddenFiles() {
            isScanHiddenFiles = false
        }

        /**
         *  扫描apk文件
         * Scan APK files
         */
        fun scanApkFiles() {
            mFilseFilterSet.add("apk")
        }


        /**
         * 扫描log文件 temp文件
         * Scan log file temp file
         */
        fun scanLogFiles() {
            mFilseFilterSet.add("log")
            mFilseFilterSet.add("temp")
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
         *Scan picture type file
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
         *Scan multimedia file type
         */
        fun scanVideoFiles() {
            mFilseFilterSet.add("mp4")
            mFilseFilterSet.add("avi")
            mFilseFilterSet.add("wmv")
            mFilseFilterSet.add("flv")
        }

        /**
         * 扫描音频文件类型
         * Scan audio file type
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

        /**
         * 检查文件后缀过滤规则 既文件类型过滤规则
         */
        private fun checkSuffixFilter(name: String): Boolean {
            if (mFilseFilterSet.isNotEmpty()) {
                //获取文件后缀
                val suffix: String =
                    name.substring(name.indexOfLast { it == '.' } + 1, name.length)
                        .toLowerCase()
                return mFilseFilterSet.contains(suffix)
            } else {
                return true
            }
        }

        /**
         * 创建过滤规则
         * Create filter rule
         */
        fun build(): FilenameFilter {
            return object : FilenameFilter {
                override fun accept(dir: File, name: String): Boolean {
                    //先检测自定义过滤规则
                    val customAcceptList: MutableList<Boolean> = mutableListOf()
                    if (customFilterList.isNotEmpty()) {
                        for (filenameFilter in customFilterList) {
                            val accept = filenameFilter.accept(dir, name)
                            customAcceptList.add(accept)
                        }
                    }

                    //隐藏文件扫描规则 优先级高
                    // isScanHiddenFiles==true 扫描隐藏文件就不判断是不是隐藏文件了
                    // isScanHiddenFiles==false 不扫描隐藏文件 判断是不是隐藏文件 是隐藏文件就过滤
                    if (!isScanHiddenFiles && dir.isHidden) {
                        return false
                    }

                    //只扫描文件夹 文件夹不需要后缀规则检查
                    if (isOnlyDir) {
                        return dir.isDirectory
                                && checkNameLikeFilter(name)
                                && checkNameNotLikeFilter(name)
                                && (customAcceptList.isEmpty() || !customAcceptList.contains(false))
                    }

                    //只扫描文件 同时应用文件扫描规则
                    if (isOnlyFile) {
                        return dir.isFile
                                && checkSuffixFilter(name)
                                && checkNameLikeFilter(name)
                                && checkNameNotLikeFilter(name)
                                && (customAcceptList.isEmpty() || !customAcceptList.contains(false))
                    }

                    //默认检查规则
                    return checkSuffixFilter(name)
                            && checkNameLikeFilter(name)
                            && checkNameNotLikeFilter(name)
                            && (customAcceptList.isEmpty() || !customAcceptList.contains(false))
                }
            }

        }

    }

}