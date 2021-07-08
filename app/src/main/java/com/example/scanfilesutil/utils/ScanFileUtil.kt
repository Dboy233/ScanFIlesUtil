package com.example.scanfilesutil.utils

import android.os.Environment
import kotlinx.coroutines.*
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
 * @sample {
 *    val scanFile = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
 *
 *          //设置过滤规则 Set up filter rules
 *          scanFile.setCallBackFilter(
 *                  ScanFileUtil.FileFilterBuilder() .apply {
 *                                  onlyScanFile()
 *                                   scanApkFiles()
 *                             }.build())
 *
 *   scanFile.setScanFileListener(object : ScanFileUtil.ScanFileListener {
 *
 *           /**
 *            * 扫描开始的时候
 *            */
 *           fun scanBegin(){
 *
 *           }
 *
 *           /**
 *            * @param timeConsuming 耗时
 *            */
 *           fun scanComplete(timeConsuming: Long){
 *
 *           }
 *           /**
 *             *@param file 扫描的文件,Scanned file
 *             */
 *          fun scanningCallBack(file: File){
 *
 *          }
 *    })
 *
 *      //开始扫描 Start scanning
 *      scanFile.startAsyncScan()
 * }
 *
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
     * 文件路径不存在
     * file path does not exist
     */
    private val PATH_NOT_EXISTS = -1L

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
     * 协程扫描任务
     * Coroutine scan task
     */
    private var mCoroutineScope: CoroutineScope? = null

    /**
     * 协程递归使用次数记录 每次递归调用都会产程一个新的进程 进程执行完成 递减1
     * Correlation recursion usage count record Each recursive call will produce a new process.
     * Process execution is completed Decrement by 1
     */
    private var mCoroutineSize = 0

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
     * @param rootPath 扫描的路径 Scanning path
     */
    constructor(rootPath: String) {
        this.mRootPath = rootPath.trimEnd { it == '/' }
    }

    /**
     * @param rootPath 扫描的路径 Scanning path
     * @param scanFileListener 完成回调接口
     */
    constructor(rootPath: String, scanFileListener: ScanFileListener) {
        this.mRootPath = rootPath.trimEnd { it == '/' }
        mScanFileListener = scanFileListener
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
        mCoroutineScope?.cancel()
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
    fun startAsyncScan() {
        if (!isStop) {
            return
        }
        isStop = false
        mCoroutineSize = 0

        //检查路径的可用性
        //Check path availability
        val file = File(mRootPath)
        if (!file.exists()) {
            mScanFileListener?.scanComplete(PATH_NOT_EXISTS)
            return
        }
        //如果协程是空的 或者已经结束过了，重新实例化协程
        //If the coroutine is empty or has ended, re-instantiate the coroutine
        if (mCoroutineScope == null || mCoroutineScope?.isActive == false) {
            mCoroutineScope = CoroutineScope(Dispatchers.IO)
        }
        mScanTime = System.currentTimeMillis()
        mScanFileListener?.scanBegin()
        //开始扫描
        //Start scanning
        asyncScan(file)
    }


    /**
     * 异步扫描文件， 递归调用
     * Scan files asynchronously, call recursively
     * @param dirOrFile 要扫描的文件 或 文件夹;The file or folder to scan
     */
    private fun asyncScan(dirOrFile: File) {
        plusCoroutineSize()
        //将任务添加到列队中 Add tasks to the queue
        mCoroutineScope?.launch(Dispatchers.IO) {
            //扫描路径层级判断 Scan path level judgment
            if (checkLevel(dirOrFile)) {
                checkCoroutineSize()
                return@launch
            }

            //检查是否是文件 是文件就直接回调 返回true
            //Check whether it is a file or a file, and call back directly to return true
            if (dirOrFile.isFile) {
                if (filterFile(dirOrFile)) {
                    mScanFileListener?.scanningCallBack(dirOrFile)
                }
                checkCoroutineSize()
                return@launch
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
                        mScanFileListener?.scanningCallBack(it)
                    }
                    //再次调用此方法
                    //Call this method again
                    asyncScan(it)
                } else {
                    //是文件,回调,验证过滤规则
                    //Is a file, callback, verification filter rules
                    if (filterFile(it)) {
                        mScanFileListener?.scanningCallBack(it)
                    }
                }
            }
            checkCoroutineSize()
            return@launch
        }
    }

    /**
     * 增加一次协程使用次数
     * Increase the use of coroutines
     */
    @Synchronized
    private fun plusCoroutineSize() {
        mCoroutineSize++
    }

    /**
     * 检查协程使用次数 减少一次
     * 如果mCoroutineSize==0说明已经扫描完了
     * Check the usage of coroutine. Decrease it once.
     * If mCoroutineSize == 0, it means it has been scanned.
     */
    @Synchronized
    private fun checkCoroutineSize() {
        mCoroutineSize--
        //如果mCoroutineSize==0,说明协程全部执行完毕，可以回调完成方法
        //If m Coroutine Size == 0, it means that all coroutines
        // have been executed and you can call back the completion
        if (mCoroutineSize == 0) {
            isStop = true
            mCoroutineScope?.launch(Dispatchers.Main) {
                mScanTime = System.currentTimeMillis() - mScanTime
                mScanFileListener?.scanComplete(mScanTime)
                mCoroutineScope?.cancel()
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
            !isStop
        } else {
            mCallBackFilter!!.accept(file, file.name) && !isStop
        }
    }

    /**
     * 获取扫描任务，
     * 不要手动调用此方法，
     * 使用ScanTogetherManager().scan()时自动调用此方法。
     *
     *  To obtain a scan task, do not call this method manually.
     *  This method is automatically called when ScanTogetherManager().Scan() is used.
     */
    private fun getScanningTask(): CoroutineScope? {
        return mCoroutineScope
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
    @Deprecated(message = "不建议使用")
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
     * 一起扫描管理类，
     * 提供扫描和停止方法。
     *
     * Used to manage scanning together, providing scanning and stopping methods.
     */
    class ScanTogetherManager {
        /**
         * 要一起扫描的任务集合Set
         * Set of tasks to scan together
         */
        private var mTogetherJob = mutableSetOf<ScanFileUtil>()

        /**
         * 检查任务是否完成的进程
         * Check whether the task is completed
         */
        private var launch: Job? = null

        /**
         * 开始扫描
         * Start scanning
         * @param arrayOfScanFileUtils 扫描任务们 Scanning tasks
         * @param allCompleteCallBack 全部完成回调
         */
        fun scan(vararg arrayOfScanFileUtils: ScanFileUtil?, allCompleteCallBack: () -> Unit) {
            //如果任务已经开始了不允许再次执行和调用
            if (launch != null && launch?.isActive == true) {
                return
            }
            //清空上次执行列表
            mTogetherJob.clear()
            //添加这次执行的任务队列
            arrayOfScanFileUtils.map {
                it?.apply {
                    mTogetherJob.add(this)
                }
            }
            //开启任务 Start task
            launch = GlobalScope.launch(Dispatchers.IO) {
                //执行扫描
                mTogetherJob.map {
                    it.stop()
                    it.startAsyncScan()
                }

                //检查所有任务是否在运行 在运行等待运行结束
                mTogetherJob.map {
                    while (it.getScanningTask()?.isActive == true);
                }
                //所有任务都结束了在main线程 回调完成函数
                withContext(Dispatchers.Main) {
                    allCompleteCallBack()
                }
            }
        }

        /**
         * 取消同时执行的扫描任务
         * Cancel simultaneous scan tasks
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
         * 清空任务列表 Clear the task list
         */
        fun clear() {
            mTogetherJob.clear()
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
         * Just scan the file
         */
        fun onlyScanFile() {
            isOnlyFile = true
        }

        /**
         * 扫描名字像它的文件或者文件夹
         * Scan names like its files or folders
         */
        fun scanNameLikeIt(like: String) {
            mNameLikeFilterSet.add(like.toLowerCase(Locale.getDefault()))
        }

        /**
         * 扫描名与其文件不同
         * 也就是说，不要扫描这样的文件
         * Scan name is not like its file
         * That is, don't scan files with names like this
         */
        fun scanNameNotLikeIt(like: String) {
            mNameNotLikeFilterSet.add(like.toLowerCase(Locale.getDefault()))
        }

        /**
         * 扫描TxT文件
         * Scan text files only
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
                    //先检查用户自定义的过滤规则 优先级最高
                    if (customFilterList.isNotEmpty()) {
                        for (filenameFilter in customFilterList) {
                            val accept = filenameFilter.accept(dir, name)
                            if (!accept) {
                                //只要有一个过滤规则不通过就停止循环
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
        fun scanBegin()

        /**
         * 在主线程回调
         * Callback in main thread
         * 扫描完成回调 Scan completion callback
         * @param timeConsuming 耗时 时间为-1说明扫描目录不存在
         */
        fun scanComplete(timeConsuming: Long)

        /**
         * 在子线程回调
         * 扫描到文件时回调，每扫描到一个文件触发一次
         * Callback in child thread
         * Callback when a file is scanned, triggered every time a file is scanned
         * @param file 扫描的文件
         */
        fun scanningCallBack(file: File)
    }

}