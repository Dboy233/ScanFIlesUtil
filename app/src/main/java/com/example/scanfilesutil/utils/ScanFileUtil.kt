package com.example.scanfilesutil.utils

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FilenameFilter
import java.lang.Deprecated

/**
 * 扫描文件工具
 * @author Dboy
 * @作者： Dboy
 * @see https://github.com/Dboy233/ScanFIlesUtil
 *
 *  设置你的gradle版本 / Set your gradle version
 *  targetSdkVersion <= 28
 *
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
     * 扫描完成回调
     */
    private var mCompleteCallBack: (() -> Unit)? = null

    /**
     * 设置扫描时回调接口
     */
    private lateinit var mScanningCallBack: suspend ((file: File) -> Unit)

    /**
     * 扫描层级
     */
    private var mScanLevel = -1L

    /**
     * 协程扫描任务
     */
    private var mCoroutineScope: CoroutineScope? = null

    /**
     * 协程递归使用次数记录 每次递归调用都会产程一个新的进程 进程执行完成 递减1
     */
    private var mCoroutineSize = 0

    /**
     * 扫描用时
     */
    private var mScanTime = 0L

    /**
     * @param rootPath 扫描的路径
     */
    constructor(rootPath: String) {
        this.mRootPath = rootPath.trimEnd { it == '/' }
    }

    /**
     * @param rootPath 扫描的路径
     * @param complete 完成回调 == completeCallBack
     */
    constructor(rootPath: String, complete: () -> Unit) {
        this.mRootPath = rootPath.trimEnd { it == '/' }
        mCompleteCallBack = complete
    }

    /**
     * 设置扫结束回调
     */
    fun setCompleteCallBack(success: () -> Unit) {
        mCompleteCallBack = success
    }

    /**
     * 设置扫描层级数
     */
    fun setScanLevel(level: Long) {
        mScanLevel = level
    }

    /**
     * 停止 只能使用变量来终端扫描。因为协程线程太多，遍历列队逐个终止貌似不是很现实。
     * 所以使用变量让各个协程自己处理。终止自己当前的扫描过程。
     */
    fun stop() {
        isStop = true
        mCoroutineScope?.cancel()
    }

    /**
     * 获取扫描耗时
     */
    fun getScanTimeConsuming() = mScanTime

    /**
     * 使用这个方法 必须调用 setScanningCallBack
     */
    fun startAsyncScan() {
        if (mScanningCallBack != null) {
            startAsyncScan(mScanningCallBack)
        }
    }


    /**
     * 开始异步扫描文件
     */
    fun startAsyncScan(callback: suspend (file: File) -> Unit) {
        if (!isStop) {
            return
        }
        isStop = false
        mCoroutineSize = 0
        mScanningCallBack = callback

        val file = File(mRootPath)
        if (!file.exists()) {
            return
        }
        //如果协程是空的 或者已经结束过了，重新实例化协程
        if (mCoroutineScope == null || mCoroutineScope?.isActive == false) {
            mCoroutineScope = CoroutineScope(Dispatchers.IO)
        }
        mScanTime = System.currentTimeMillis()
        //开始扫描
        asyncScan(file, callback)
    }

    /**
     * 设置扫描时回调
     */
    fun setScanningCallBack(callback: suspend (file: File) -> Unit) {
        mScanningCallBack = callback
    }

    /**
     * 异步扫描文件 递归调用
     * @param dirOrFile 要扫描的文件 或 文件夹
     * @param callback 文件回调 再子线程中 不可操作UI 将扫描到的文件通过callback调用
     */
    private fun asyncScan(dirOrFile: File, callback: suspend (file: File) -> Unit) {
        plusCoroutineSize()
        //将任务添加到列队中
        mCoroutineScope?.launch(Dispatchers.IO) {
            //扫描路径层级判断
            if (checkLevel(dirOrFile)) {
                checkCoroutineSize()
                return@launch
            }

            //检查是否是文件 是文件就直接回调 返回true
            if (dirOrFile.isFile) {
                if (filterFile(dirOrFile)) {
                    callback(dirOrFile)
                }
                checkCoroutineSize()
                return@launch
            }
            //获取文件夹中的文件集合
            val rootFile = getFilterFilesList(dirOrFile)
            //遍历文件夹
            rootFile?.map {
                //如果是文件夹 回调 递归调用函数 再遍历判断
                if (it.isDirectory) {
                    if (filterFile(it)) {
                        callback(it)
                    }
                    //再次调用自己
                    asyncScan(it, callback)
                } else {
                    //是文件 回调
                    //验证过滤规则
                    if (filterFile(it)) {
                        callback(it)
                    }
                }
            }
            checkCoroutineSize()
            return@launch
        }
    }

    /**
     * 增加一次协程使用次数
     */
    @Synchronized
    private fun plusCoroutineSize() {
        mCoroutineSize++
    }

    /**
     * 检查协程使用次数 减少一次
     * 如果mCoroutineSize==0说明已经扫描完了
     */
    @Synchronized
    private fun checkCoroutineSize() {
        mCoroutineSize--
        if (mCoroutineSize == 0) {
            isStop = true
            if (mCompleteCallBack != null) {
                mCoroutineScope?.launch(Dispatchers.Main) {
                    mScanTime = System.currentTimeMillis() - mScanTime
                    mCompleteCallBack?.invoke()
                    mCoroutineScope?.cancel()
                }
            }
        }
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
     * 不要手动调用此方法
     * 获取扫描任务完成回调
     * 使用ScanFileUtil.awaitMultiScan()时调用此方法
     */
    private fun getScanningQueueAsync(): CoroutineScope? {
        return mCoroutineScope
    }

    /**
     *  文件通过callback返回结果时过滤规则
     *  @param filter 使用FileFilterBuilder设置过滤规则
     */
    fun setCallBackFilter(filter: FilenameFilter?) {
        this.mCallBackFilter = filter
    }

    /**
     * 扫描时过滤规则
     * @Deprecated use {@link setCallBackFilter}
     */
    @Deprecated
    fun setScanningFilter(filter: FilenameFilter?) {
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
     * 一起扫描管理类
     * 提供扫描和停止方法
     */
    class ScanTogetherManager {
        /**
         * 要一起扫描的任务集合Set
         */
        private var mTogetherJob = mutableSetOf<ScanFileUtil>()

        /**
         * 检查任务是否完成的进程
         */
        private var launch: Job? = null

        /**
         * 全部完成回调
         */
        private var mCompleteCallBack: (() -> Unit)? = null

        /**
         * 开始扫描
         * @param deferred 扫描任务
         * @param complete 完成回调
         */
        fun scan(vararg deferred: ScanFileUtil?, complete: () -> Unit) {
            //如果任务已经开始了不允许再次执行和调用
            if (launch != null && launch?.isActive == true) {
                return
            }
            //清空上次执行列表
            mTogetherJob.clear()
            //添加这次执行的任务队列
            deferred.map {
                it?.apply {
                    mTogetherJob.add(this)
                }
            }
            //确认完成回调
            mCompleteCallBack = complete
            //开启任务
            launch = GlobalScope.launch(Dispatchers.IO) {
                //执行扫描
                mTogetherJob.map {
                    it.stop()
                    it.startAsyncScan()
                }

                //检查所有任务是否在运行 在运行等待运行结束
                mTogetherJob.map {
                    while (it.getScanningQueueAsync()?.isActive == true);
                }
                //所有任务都结束了在main线程 回调完成函数
                withContext(Dispatchers.Main) {
                    mCompleteCallBack?.invoke()
                }
            }
        }

        /**
         * 取消同时执行的扫描任务
         */
        fun cancel() {
            //先取消当前的任务
            launch?.cancel()
            //依次取消同行的任务
            for (scanFileUtil in mTogetherJob) {
                scanFileUtil.stop()
            }
            //清空任务列表
            mTogetherJob.clear()
        }

        /**
         * 清空任务列表
         */
        fun clear() {
            mTogetherJob.clear()
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