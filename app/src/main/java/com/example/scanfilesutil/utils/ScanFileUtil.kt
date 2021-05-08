package com.example.scanfilesutil.utils

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FilenameFilter
import java.util.*

/**
 * 扫描文件工具
 * Scan file tool
 *
 * @author Dboy
 * @作者： Dboy
 * @see  'https://github.com/Dboy233/ScanFIlesUtil'
 * @see 'targetSdkVersion'  targetSdkVersion <= 28 ; 设置你的gradle版本 / Set your gradle version
 */

@Suppress("unused")
class ScanFileUtil {

    companion object {
        //手机外部存储根目录 Mobile storage root directory
        val externalStorageDirectory: String by lazy {
            Environment.getExternalStorageDirectory().absolutePath
        }

        //手机app缓存存放路径 Mobile app cache storage path
        val android_app_data_folder: String by lazy {
            "$externalStorageDirectory/Android/data"
        }
    }

    /**
     * 是否停止扫描
     * Whether to stop scanning
     */
    private var isStop = true

    /**
     * 要扫描的根路径
     * Root path to scan
     */
    private val mRootPath: String

    /**
     * 扫描到文件回调过滤规则
     * Scan to file callback filter rule
     */
    private var mCallBackFilter: FilenameFilter? = null

    /**
     * 扫描时的过滤规则 只建议用来过滤隐藏文件和大小为0的文件
     * The filtering rules during scanning are only recommended to filter hidden files and files of size 0
     */
    private var mScanFilter: FilenameFilter? = null

    /**
     * 扫描层级
     * Scan level
     */
    private var mScanLevel = -1L

    /**
     * 扫描用时
     * Scan time
     */
    private var mScanTime = 0L

    /**
     * 文件扫描回调
     * File scanning callback
     */
    private var mScanFileListener: ScanFileListener? = null

    /**
     * flow 扫描任务
     */
    private var mJobFlowScan: Flow<File>? = null

    /**
     * 当手动调用 stop() 函数的时候是否 在之后调用onComplete()函数
     * 是否开启主动停止后触发完成回调
     *
     */
    var enableCallComplete = false


    /**
     * @param rootPath 扫描的路径 Scanning path
     */
    constructor(rootPath: String) {
        this.mRootPath = rootPath.trimEnd { it == '/' }
    }

    /**
     * 设置扫描监听器 ，Set Scan Listener
     */
    fun setScanFileListener(scanFileListener: ScanFileListener) {
        mScanFileListener = scanFileListener
    }

    /**
     * 设置扫描层级数
     * Set the number of scan levels
     */
    fun setScanLevel(level: Long) {
        mScanLevel = level
    }

    /**
     * 停止扫描
     * Stop scanning
     */
    fun stop() {
        isStop = true
        mJobFlowScan?.cancellable()
    }

    fun enableStopCallComplete(enable: Boolean) {
        enableCallComplete = enable
    }

    /**
     * 获取扫描耗时
     *Time to get a scan
     */
    fun getScanTimeConsuming() = mScanTime

    /**
     * 开始异步扫描文件
     * Start scanning files asynchronously
     */
    fun startScan() {
        //还没停止不允许重复调用
        if (!isStop) {
            return
        }
        isStop = false
        //检查路径的可用性
        //Check path availability
        val file = File(mRootPath)
        if (!file.exists()) {
            return
        }
        mJobFlowScan = flow<File> {
            recursionScan(file, this)
            onComplete()
        }.onStart {
            mScanFileListener?.onBegin()
            mScanTime = System.currentTimeMillis()
        }.catch {
            mScanFileListener?.onError()
        }.flowOn(Dispatchers.IO)
        //在主线程回调
        GlobalScope.launch(Dispatchers.Main) {
            mJobFlowScan?.collect {
                mScanFileListener?.onFile(it)
            }
        }
    }

    /**
     * 完成
     */
    private fun onComplete() {
        if (isStop && !enableCallComplete) {
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            mScanTime = System.currentTimeMillis() - mScanTime
            mScanFileListener?.onComplete(mScanTime)
            isStop = true
        }
    }

    /**
     *
     * 递归扫描
     * Recursive scan
     * @param dirOrFile 要扫描的文件 或 文件夹;The file or folder to scan
     */
    private suspend fun recursionScan(dirOrFile: File, flow: FlowCollector<File>) {
        //扫描路径层级判断 Scan path level judgment
        if (checkLevel(dirOrFile)) {
            return
        }
        //检查是否是文件 是文件就直接回调 返回true
        //Check whether it is a file or a file, and call back directly to return true
        if (dirOrFile.isFile) {
            if (filterFile(dirOrFile)) {
                flow.emit(dirOrFile)
            }
            return
        }
        //获取文件夹中的文件集合
        //Get a collection of files in a folder
        val rootFile = getFilterFilesList(dirOrFile)
        //遍历文件夹
        //Get a collection of files in a folder
        rootFile?.map {
            //如果是文件夹-回调, 调用自己,再遍历扫描
            //If it is a folder-callback, call yourself and then traverse the scan
            if (it.isDirectory) {
                if (filterFile(it)) {
                    flow.emit(it)
                }
                //再次调用此方法
                //Call this method again
                recursionScan(it, flow)
            } else {
                //是文件,回调,验证过滤规则
                //Is a file, callback, verification filter rules
                if (filterFile(it)) {
                    flow.emit(it)
                }
            }
        }
    }

    /**
     * 检查扫描路径是否已经到指定的层级
     * Check if the scan path has reached the specified level
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
     * Check filter file
     */
    private fun filterFile(file: File): Boolean {
        return if (mCallBackFilter == null) {
            true
        } else {
            mCallBackFilter!!.accept(file, file.name)
        }
    }

    /**
     *  文件通过callback返回结果时过滤规则
     *  Filter rules when the file returns results through callback
     *  @param filter 使用FileFilterBuilder设置过滤规则，Use File Filter Builder to set filter rules
     */
    fun setCallBackFilter(filter: FilenameFilter?) {
        this.mCallBackFilter = filter
    }

    /**
     * 扫描时过滤规则
     * 过滤速度很快，但是可能会过滤掉一些父级文件夹，它的子文件夹将不会被扫描
     * Filter rules when scanning.
     * The filtering speed is fast, but some parent folders may be filtered out,
     * and its subfolders will not be scanned
     * @Deprecated use {@link setCallBackFilter}
     */
    @Deprecated("Use setCallBackFilter")
    fun setScanningFilter(filter: FilenameFilter?) {
        this.mScanFilter = filter
    }


    /**
     * 获取文件夹中的文件列表 并且引用过滤规则
     * Get a list of files in a folder and reference filter rules
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
     * Filter builder
     */
    class FileFilterBuilder {
        /**
         * 添加自定义filter规则 集合
         * Add custom filter rules collection
         */
        private val customFilterList: MutableList<FilenameFilter> = mutableListOf()

        /**
         * 文件类型&文件后缀 扫描过滤规则 集合
         * File type & file suffix Scan filter rules Collection
         */
        private val mFilseFilterSet: MutableSet<String> = hashSetOf()

        /**
         * 扫描名字像它的 集合
         * Scan the name like its collection
         */
        private val mNameLikeFilterSet: MutableSet<String> = hashSetOf()

        /**
         * 扫描名字不像它的文件 集合,
         * 也就是不扫描名字像这个的文件 集合
         * Scan a collection of files whose name is not like it,
         * that is, do not scan a collection of files whose name is like this
         */
        private val mNameNotLikeFilterSet: MutableSet<String> = hashSetOf()

        /**
         * 是否扫描隐藏文件 true扫描 false不扫描
         */
        private var isScanHiddenFiles = true

        /**
         * 只要扫描文件
         * Just scan the file
         */
        private var isOnlyFile = false

        /**
         * 只扫描文件夹
         * Scan folders only
         */
        private var isOnlyDir = false


        /**
         * 添加自定义filter规则
         * Add custom filter rule
         */
        fun addCustomFilter(filter: FilenameFilter): FileFilterBuilder {
            customFilterList.add(filter)
            return this
        }

        /**
         * 只扫描文件夹
         * Scan folders only
         */
        fun onlyScanDir(): FileFilterBuilder {
            isOnlyDir = true
            return this
        }

        /**
         * 只要扫描文件
         * Just scan the file
         */
        fun onlyScanFile(): FileFilterBuilder {
            isOnlyFile = true
            return this
        }

        /**
         * 扫描名字像它的文件或者文件夹
         * Scan names like its files or folders
         */
        fun scanNameLikeIt(like: String): FileFilterBuilder {
            mNameLikeFilterSet.add(like.toLowerCase(Locale.getDefault()))
            return this
        }

        /**
         * 扫描名与其文件不同
         * 也就是说，不要扫描这样的文件
         * Scan name is not like its file
         * That is, don't scan files with names like this
         */
        fun scanNameNotLikeIt(like: String): FileFilterBuilder {
            mNameNotLikeFilterSet.add(like.toLowerCase(Locale.getDefault()))
            return this
        }

        /**
         * 扫描TxT文件
         * Scan text files only
         */
        fun scanTxTFiles(): FileFilterBuilder {
            mFilseFilterSet.add("txt")
            return this
        }

        /**
         * 不扫描隐藏文件
         * Don't scan hidden files
         */
        fun notScanHiddenFiles(): FileFilterBuilder {
            isScanHiddenFiles = false
            return this
        }

        /**
         *  扫描apk文件
         * Scan APK files
         */
        fun scanApkFiles(): FileFilterBuilder {
            mFilseFilterSet.add("apk")
            return this
        }


        /**
         * 扫描log文件 temp文件
         * Scan log file temp file
         */
        fun scanLogFiles(): FileFilterBuilder {
            mFilseFilterSet.add("log")
            mFilseFilterSet.add("temp")
            return this
        }

        /**
         * 扫描文档类型文件
         */
        fun scanDocumentFiles(): FileFilterBuilder {
            mFilseFilterSet.add("txt")
            mFilseFilterSet.add("pdf")
            mFilseFilterSet.add("doc")
            mFilseFilterSet.add("docx")
            mFilseFilterSet.add("xls")
            mFilseFilterSet.add("xlsx")
            return this
        }

        /**
         * 扫描图片类型文件
         *Scan picture type file
         */
        fun scanPictureFiles(): FileFilterBuilder {
            mFilseFilterSet.add("jpg")
            mFilseFilterSet.add("jpeg")
            mFilseFilterSet.add("png")
            mFilseFilterSet.add("bmp")
            mFilseFilterSet.add("gif")
            return this
        }

        /**
         * 扫描多媒体文件类型
         *Scan multimedia file type
         */
        fun scanVideoFiles(): FileFilterBuilder {
            mFilseFilterSet.add("mp4")
            mFilseFilterSet.add("avi")
            mFilseFilterSet.add("wmv")
            mFilseFilterSet.add("flv")
            return this
        }

        /**
         * 扫描音频文件类型
         * Scan audio file type
         */
        fun scanMusicFiles(): FileFilterBuilder {
            mFilseFilterSet.add("mp3")
            mFilseFilterSet.add("ogg")
            return this
        }

        /**
         * 扫描压缩包文件类型
         */
        fun scanZipFiles(): FileFilterBuilder {
            mFilseFilterSet.add("zip")
            mFilseFilterSet.add("rar")
            mFilseFilterSet.add("7z")
            return this
        }

        /**
         * 检查名字相似过滤
         */
        private fun checkNameLikeFilter(name: String): Boolean {
            //相似名字获取过滤
            if (mNameLikeFilterSet.isNotEmpty()) {
                mNameLikeFilterSet.map {
                    if (name.toLowerCase(Locale.getDefault()).contains(it)) {
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
                    if (name.toLowerCase(Locale.getDefault()).contains(it)) {
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
            return if (mFilseFilterSet.isNotEmpty()) {
                //获取文件后缀
                val suffix: String =
                    name.substring(name.indexOfLast { it == '.' } + 1, name.length)
                        .toLowerCase(Locale.getDefault())
                //return 是否包含这个文件
                mFilseFilterSet.contains(suffix)
            } else {
                //如果没有设置这个规则，全部默认为true 全部通过
                true
            }
        }

        /**
         * 重置构建器
         */
        fun resetBuild() {
            mFilseFilterSet.clear()
            mNameLikeFilterSet.clear()
            mNameNotLikeFilterSet.clear()
            customFilterList.clear()
            isScanHiddenFiles = true
            isOnlyDir = false
            isOnlyFile = false
        }

        /**
         * 创建过滤规则
         * Create filter rule
         */
        fun build(): FilenameFilter {
            return object : FilenameFilter {

                override fun accept(dir: File, name: String): Boolean {
                    //先检测自定义过滤规则
                    if (customFilterList.isNotEmpty()) {
                        for (filenameFilter in customFilterList) {
                            val accept = filenameFilter.accept(dir, name)
                            if (!accept) {
                                return false
                            }
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
                    }

                    //只扫描文件 同时应用文件扫描规则
                    if (isOnlyFile) {
                        return dir.isFile
                                && checkSuffixFilter(name)
                                && checkNameLikeFilter(name)
                                && checkNameNotLikeFilter(name)
                    }

                    //默认检查规则
                    return checkSuffixFilter(name)
                            && checkNameLikeFilter(name)
                            && checkNameNotLikeFilter(name)
                }
            }

        }

    }

    /**
     * 扫描文件监听器
     * Scanning file listener
     */
    interface ScanFileListener {

        /**
         * 在子线程回调
         * Callback in child thread
         * 扫描开始的时候 描述
         */
        fun onBegin()

        /**
         * 在主线程回调
         * Callback in main thread
         * 扫描完成回调 Scan completion callback
         * @param timeConsuming 耗时
         */
        fun onComplete(timeConsuming: Long)

        /**
         * 当扫描报错
         */
        fun onError()

        /**
         * 在子线程回调
         * 扫描到文件时回调，每扫描到一个文件触发一次
         * Callback in child thread
         * Callback when a file is scanned, triggered every time a file is scanned
         * @param file 扫描的文件
         */
        fun onFile(file: File)
    }

    open class ScanFileListenerAdapter : ScanFileListener {

        override fun onBegin() {

        }

        override fun onComplete(timeConsuming: Long) {

        }

        override fun onError() {

        }

        override fun onFile(file: File) {

        }

    }

}