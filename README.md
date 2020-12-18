# ScanFileUtil - Flow

 使用协程`Flow`扫描手机文件，可用来扫描垃圾文件，缓存文件，应用缓存，视频文件，音频文件，文本文件，等等.

 Using coroutine `Flow` program to scan Android mobile files can be used to scan junk files, cache files, application cache, video files, audio files, text files, etc.

 [`有疑问或者bug 提交 issues,我会及时休整`](https://github.com/Dboy233/ScanFIlesUtil/issues)

 [`Submit issues if you have questions or bugs,I will modify it in time.`](https://github.com/Dboy233/ScanFIlesUtil/issues)



##### 使用方式：

粘贴 [ScanFileUtil.kt](https://github.com/Dboy233/ScanFIlesUtil/blob/flow-scan/app/src/main/java/com/example/scanfilesutil/utils/ScanFileUtil.kt )使用

Paste [ScanFileUtil.kt](https://github.com/Dboy233/ScanFIlesUtil/blob/flow-scan/app/src/main/java/com/example/scanfilesutil/utils/ScanFileUtil.kt) to use

```kotlin
	//扫描任务
 	val scanJob: ScanFileUtil = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
	//设置过滤规则
	val filter = ScanFileUtil.FileFilterBuilder().onlyScanDir().build()
	//设置过滤规则
	scanJob.setCallBackFilter(filter)
	//设置回调
    scanJob.setScanFileListener(object : ScanFileUtil.ScanFileListener {
        override fun onBegin() {
          //开始的时候
        }
        override fun onComplete(timeConsuming: Long) {
            //扫描完成
        }
        override fun onError() {
            //当扫描出现错误的时候。
        }
        override fun onFile(file: File) {
           //处理扫描的的文件们
        }
    })
	//启动扫描
	scanJob.startScan()
```

##### function/function introduction

`ScanFileUtil` function

| APi                                                       | describe                                                     |
| --------------------------------------------------------- | ------------------------------------------------------------ |
| `ScanFileUtil(rootPath)`                                  | rootPath=需要扫描的路径<br />rootPath=Path to scan           |
| `setScanFileListener(scanFileListener: ScanFileListener)` | 设置扫描监听器 ，Set Scan Listener                           |
| `startScan()`                                             | 开始异步扫描文件<br />Start asynchronous file scan           |
| `setScanLevel(level: Long)`                               | 设置文件夹扫描层级数。从当前目录向下扫描几层文件夹<br />Sets the number of scan levels for the folder. |
| `setCallBackFilter(filter: FilenameFilter?)`              | 设置文件扫描返回结果时过滤规则<br />Set file filtering rules，Scanning callback results |
| `setScanningFilter(filter: FilenameFilter?)`              | 扫描时过滤规则，不建议使用此方法，用`setCallBackFilter()`代替。<br />Filter rule during scanning, not recommended，<br />Replace with `setCallBackFilter()` |
| `enableStopCallComplete(enable: Boolean)`                 | 主动停止的时候是否执行完成回调<br/>                          |
| `stop()`                                                  | 停止扫描，Stop scanning                                      |



`ScanFileListener` function

| 接口方法/Interface function           | 描述/description                                             |
| ------------------------------------- | ------------------------------------------------------------ |
| `fun onBegin()`                       | 扫描开始的时候 /When the scan starts                         |
| `fun onError()`                       | 扫描出现错误                                                 |
| `fun onComplete(timeConsuming: Long)` | 扫描完成回调/Scan completion callback                        |
| `fun onFile(file: File)`              | 扫描到文件时回调，每扫描到一个文件触发一次<br>Callback when a file is scanned, <br>triggered every time a file is scanned |



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
