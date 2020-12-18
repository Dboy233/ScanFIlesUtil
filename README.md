# ScanFileUtil

### 分支 [flow-scan](https://github.com/Dboy233/ScanFIlesUtil/tree/flow-scan) 使用协程Flow实现扫描任务，如需请切换分支至 [flow-scan](https://github.com/Dboy233/ScanFIlesUtil/tree/flow-scan)n; 扫描速度不如组分支代码。

### Branch [flow-scan](https://github.com/Dboy233/ScanFIlesUtil/tree/flow-scan) uses coroutine flow to realize the scanning task. If necessary, please switch to [flow-scan](https://github.com/Dboy233/ScanFIlesUtil/tree/flow-scan); scanning speed is not as fast as main branch code.

 使用协程扫描手机文件，可用来扫描垃圾文件，缓存文件，应用缓存，视频文件，音频文件，文本文件，等等.

 Using coroutine program to scan Android mobile files can be used to scan junk files, cache files, application cache, video files, audio files, text files, etc.

 [`有疑问或者bug 提交 issues,我会及时休整`](https://github.com/Dboy233/ScanFIlesUtil/issues)

 [`Submit issues if you have questions or bugs,I will modify it in time.`](https://github.com/Dboy233/ScanFIlesUtil/issues)


#### [原理解析 https://juejin.im/post/5eb903355188256d7e066e90](https://juejin.im/post/5eb903355188256d7e066e90)
 
##### 使用方式：

粘贴[ScanFileUtil.kt](https://github.com/Dboy233/ScanFIlesUtil/blob/master/app/src/main/java/com/example/scanfilesutil/utils/ScanFileUtil.kt)使用

Paste [ScanFileUtil.kt](https://github.com/Dboy233/ScanFIlesUtil/blob/master/app/src/main/java/com/example/scanfilesutil/utils/ScanFileUtil.kt) to use

```kotlin
 val scanFile = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
       //设置过滤规则 Set up filter rules
       scanFile.setCallBackFilter(
               ScanFileUtil.FileFilterBuilder() .apply {
                               onlyScanFile()//只扫描文件
                                scanApkFiles()//只扫描APK文件
                   //...
                          }.build())

scanFile.setScanFileListener(object : ScanFileUtil.ScanFileListener {
        /**
         * 扫描开始的时候
         */
        fun scanBegin(){
        }
        /**
         * 扫描完成 Scan Complete
         * @param timeConsuming 耗时
         */
        fun scanComplete(timeConsuming: Long){
        }
        /**
          * @param file 扫描的文件,Scanned file
          */
       fun scanningCallBack(file: File){
       }
 })
   //开始扫描 Start scanning
   scanFile.startAsyncScan()
}
```

##### function/function introduction

`ScanFileUtil` function

| APi                                                          | describe                                                     |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| `ScanFileUtil(rootPath)`                                     | rootPath=需要扫描的路径<br />rootPath=Path to scan           |
| `ScanFileUtil(rootPath: String, scanFileListener: ScanFileListener)` | rootPath=需要扫描的路径<br />rootPath=Path to scan<br />scanFileListener= `setScanFileListener`  Scan Listener |
| `setScanFileListener(scanFileListener: ScanFileListener)`    | 设置扫描监听器 ，Set Scan Listener                           |
| `startAsyncScan()`                                           | 开始异步扫描文件<br />配合 `setScanningCallBack`使用<br />Start asynchronous file scan<br />must use `setScanningCallBack` |
| `setScanLevel(level: Long)`                                  | 设置文件夹扫描层级数。从当前目录向下扫描几层文件夹<br />Sets the number of scan levels for the folder. |
| `setCallBackFilter(filter: FilenameFilter?)`                 | 设置文件扫描返回结果时过滤规则<br />Set file filtering rules，Scanning callback results |
| `setScanningFilter(filter: FilenameFilter?)`                 | 扫描时过滤规则，不建议使用此方法，用`setCallBackFilter()`代替。<br />Filter rule during scanning, not recommended，<br />Replace with `setCallBackFilter()` |
| `stop()`                                                     | 停止扫描，Stop scanning                                      |

`ScanFileListener` function

| 接口方法/Interface function             | 描述/description                                             |
| --------------------------------------- | ------------------------------------------------------------ |
| `fun scanBegin()`                       | 扫描开始的时候 /When the scan starts                         |
| `fun scanComplete(timeConsuming: Long)` | 扫描完成回调/Scan completion callback                        |
| `fun scanningCallBack(file: File)`      | 扫描到文件时回调，每扫描到一个文件触发一次<br>Callback when a file is scanned, <br>triggered every time a file is scanned |



`ScanTogetherManager` function

| 方法/function                                                | 描述/description                                      |
| ------------------------------------------------------------ | ----------------------------------------------------- |
| `fun scan(vararg arrayOfScanFileUtils: ScanFileUtil?, allCompleteCallBack: () -> Unit)` | 开始扫描/Startscanning                                |
| `fun cancel()`                                               | 取消同时执行的扫描任务/Cancel simultaneous scan tasks |
| `fun clear()`                                                | 清空任务列表/Clear the task list                      |



##### 可以一个或多个扫描任务一起工作/Can work with one or more scan tasks

```kotlin
val oneFileList = mutableListOf<File>()
val twoFileList = mutableListOf<File>()


val scanFileOne = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
val scanFileTwo = ScanFileUtil(ScanFileUtil.externalStorageDirectory)

var mScanTogetherManager = ScanFileUtil.ScanTogetherManager()

scanFileOne.setScanFileListener(object : ScanFileUtil.ScanFileListener {
        
            override fun scanBegin() {
                oneFileList.clear()
            }

            override fun scanComplete(timeConsuming: Long) {
                //   处理你的扫描结果 Process your scan results
                //   Log.d("tow Scan",oneFileList.toString())
           Toast.makeText(this, "one scan end 扫描完成", Toast.LENGTH_SHORT).show()
            }

            override fun scanningCallBack(file: File) {
                oneFileList.add(file)//保存扫描数据 Save scan data
                //如果有耗时操作和计算操作，会影响扫描速度，Log也不要写在这里
                // if there are time-consuming operations and calculation operations,
                // it will affect the scanning speed，Log also don't write here
                Log.d("one Scan", "${file.absolutePath}")
            }

        })
scanFileOne.startAsyncScan()


scanFileTwo = ScanFileUtil(ScanFileUtil.externalStorageDirectory,
            object : ScanFileUtil.ScanFileListener {
       
                override fun scanBegin() {
                    twoFileList.clear()
                }

                override fun scanComplete(timeConsuming: Long) {
                    //   处理你的扫描结果 Process your scan results
                    //   Log.d("tow Scan",twoFileList.toString())
                   Toast.makeText(this, "two scan end 扫描完成", Toast.LENGTH_SHORT).show()
                }

                override fun scanningCallBack(file: File) {
                    twoFileList.add(file)//保存扫描数据 Save scan data
                    // 如果有耗时操作和计算操作，会影响扫描速度，Log也不要写在这里
                    // if there are time-consuming operations and calculation operations,
                    // it will affect the scanning speed，Log also don't write here
                    Log.d("two Scan", "${file.absolutePath}}")
                }
            })
scanFileTwo.startAsyncScan()


//======================================================
//使用这个方法同时开启多个扫描任务 Use this method to start multiple scanning tasks at the same time
    mScanTogetherManager.scan(scanFileOne, scanFileTwo) {
            Log.d("Scan", "one scan and two scan end,扫描1 和 扫描2 完成")
            Toast.makeText(
                applicationContext,
                "one scan and two scan end,扫描1 和 扫描2 完成",
                Toast.LENGTH_SHORT
            ).show()
        }
```



##### 文件扫描过滤Builder/ File scan filter builder

```kotlin
//FileFilterBuilder


/**
 * 添加自定义filter规则
 * Add custom filter rule
 */
fun addCustomFilter(filter: FilenameFilter) 

/**
 * 只扫描文件夹
 * Scan folders only
 */
fun onlyScanDir()

/**
 * 只扫描文件
 * Scan files only
 */
fun onlyScanFile()

/**
 * 扫描名字像它的文件或者文件夹
 * Scan names like its files or folders
 */
fun scanNameLikeIt(like: String)

/**
 * 扫描名字不像它的文件
 * 也就是不扫描名字像这个的文件
 *  Scan name is not like its file
 *	That is, don't scan files with names like this
 *
 */
fun scanNameNotLikeIt(like: String)

/**
* 扫描TxT文件
*Scan txt files
*/
fun scanTxTFiles()

/**
 * 不扫描隐藏文件
 * Don't scan hidden files
 */
fun notScanHiddenFiles()

/**
 *  扫描apk文件  
 * Scan APK files
 */
fun scanApkFiles()

/**
 * 扫描log文件 temp文件
 * Scan log file temp file
 */
fun scanLogFiles()
/**
* 扫描文档类型文件
* Scan document type files
*/
fun scanDocumentFiles()
/**
 * 扫描图片类型文件
 *Scan picture type file
 */
fun scanPictureFiles()

/**
 * 扫描多媒体文件类型
 *Scan multimedia file type
 */
fun scanVideoFiles() 
/**
 * 扫描音频文件类型
 * Scan audio file type
 */
fun scanMusicFiles()
/**
 * 扫描压缩包文件类型
 * Scan zip file type
 */
fun scanZipFiles()
/**
 * 创建过滤规则
 * Create filter rule
 */
fun build(): FilenameFilter
```



##### 如何使用FileFilterBuilder\How to use FileFilterBuilder

```kotlin
val scanFile = ScanFileUtil(ScanFileUtil.externalStorageDirectory)

//use
var fileFilterBuilder= ScanFileUtil.FileFilterBuilder().apply {
	onlyScanFile()
    scanNameLikeIt("ImFile")
    scanDocumentFiles()
    scanPictureFiles()
	addCustomFilter(FilenameFilter { dir, name ->
 		//Filter files with file size 0
    	//过滤文件大小为0的文件。。
		return	FileUtils.getFileLength(dir) != 0L
	})
	addCustomFilter(FilenameFilter { dir, name ->
		//.....自定义规则/other Filter
	})

}.build()

scanFile.setCallBackFilter(fileFilterBuilder)//<<<<<<< 使用/Use 

scanFile.startAsyncScan()
```

