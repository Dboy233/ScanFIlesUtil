# ScanFileUtil

### 使用协程扫描手机文件，可用来扫描垃圾文件，缓存文件，应用缓存，视频文件，音频文件，文本文件，等等。

### Using coroutine program to scan Android mobile files can be used to scan junk files, cache files, application cache, video files, audio files, text files, etc.

原理解析：https://juejin.im/post/5e0c3c7d6fb9a0482a60d26f

### [`有疑问或者bug 提交 issues,我会及时休整`](https://github.com/Dboy233/ScanFIlesUtil/issues)

### [`Submit issues if you have questions or bugs,I will modify it in time.`](https://github.com/Dboy233/ScanFIlesUtil/issues)

### 使用方式：

粘贴ScanFileUtil.kt使用

Paste ScanFileUtil.kt to use

```kotlin
val scanFile = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
//设置扫描完成回调
//Set scan completion callback
scanFile.completeCallBack {
	Toast.makeText(this, "scan end 扫描完成", Toast.LENGTH_LONG).show()
	Log.d("ScanFileUtil","$fileList")
}
//开始异步扫描文件，注意：不要用此方法更新UI
//Start to scan files asynchronously, note: do not use this method to update UI
scanFile.startAsyncScan {
	fileList.add(it)
}
```

### 基础使用Api方法

| APi                                                          | describe                                                     |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| ScanFileUtil(rootPath)                                       | rootPath=需要扫描的路径<br />rootPath=Path to scan           |
| ScanFileUtil(rootPath){ <br />          //completeCallBack<br />} | rootPath=需要扫描的路径<br />rootPath=Path to scan<br />completeCallBack |
| completeCallBack(success: () -> Unit)                        | 设置完成回调与第二个构造函数相同<br />Setting the completion callback<br /> is the same as the second constructor |
| setScanLevel(level: Long)                                    | 设置文件夹扫描层级数。从当前目录向下扫描几层文件夹<br />Sets the number of scan levels for the folder. |
| startAsyncScan(callback: (file: File) -> Unit)               | 开始异步扫描文件<br />Start asynchronous file scan           |
| suspend await()                                              | 等待扫描任务完成，不要尝试调用此方法！<br />Wait for the scan task to complete, do not attempt to call this method! |
| getAwaitInstance()                                           | 获取扫描任务完成回调<br />使用ScanFileUtil.await()时调用此方法<br />Get scan task completion callback。<br />Call this method when using ScanFileUtil.await() |
| setCallBackFilter(filter: FilenameFilter?)                   | 设置文件过滤规则，返回结果时应用<br />Set file filtering rules，Apply when returning results |
| setScanFilter(filter: FilenameFilter?)                       | 扫描时过滤规则，不建议使用此方法，用setCallBackFilter()代替。<br />Filter rule during scanning, not recommended，<br />Replace with setCallBackFilter() |

### 多个扫描任务一起工作，Multiple scan tasks working together

```kotlin
val scanFile1 = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
val scanFile2 = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
scanFile1.startAsyncScan{
    Log.d("ScanFile1","$it")
}
scanFile2.startAsyncScan{
    Log.d("scanFile2","$it")
}

ScanFileUtil.await(scanFile1.getAwaitInstance(),scanFile2.getAwaitInstance()){
    //两个任务全部完成后在UI回调此方法
    //After both tasks are completed, call back this method in UI
    Toast.makeText(this, "ScanFile1 scanFile2 all completed", Toast.LENGTH_LONG).show()
}
```



### 文件扫描过滤Builder

### File scan filter builder

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

#### 如何使用FileFilterBuilder

#### How to use FileFilterBuilder

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
		//.....other Filter
	})

}.build()

scanFile.apply {
    setScanLevel(2)
    setCallBackFilter(fileFilterBuilder)//use
}
scanFile.startAsyncScan{
    Log.d("ScanFile","$it")
}
```

