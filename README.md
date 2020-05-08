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

| APi                                                    | describe                                                     |
| ------------------------------------------------------ | ------------------------------------------------------------ |
| `ScanFileUtil(rootPath)`                               | rootPath=需要扫描的路径<br />rootPath=Path to scan           |
| `ScanFileUtil(rootPath: String, complete: () -> Unit)` | rootPath=需要扫描的路径<br />rootPath=Path to scan<br />complete = `setCompleteCallBack`  you know it |
| `setCompleteCallBack(success: () -> Unit)`             | 设置完成回调与第二个构造函数相同<br />Setting the completion callback<br /> is the same as the second constructor |
| `setScanningCallBack(callback: (file: File) -> Unit)`  | 设置扫描时的回调 <br/>Sets the callback for the scan<br/>    |
| `startAsyncScan()`                                     | 开始异步扫描文件<br />配合 `setScanningCallBack`使用<br />Start asynchronous file scan<br />must use `setScanningCallBack` |
| `startAsyncScan(callback: (file: File) -> Unit)`       | 开始异步扫描文件<br />Start asynchronous file scan           |
| `setScanLevel(level: Long)`                            | 设置文件夹扫描层级数。从当前目录向下扫描几层文件夹<br />Sets the number of scan levels for the folder. |
| `setCallBackFilter(filter: FilenameFilter?)`           | 设置文件扫描返回结果时过滤规则<br />Set file filtering rules，Scanning callback results |
| `setScanningFilter(filter: FilenameFilter?)`           | 扫描时过滤规则，不建议使用此方法，用`setCallBackFilter()`代替。<br />Filter rule during scanning, not recommended，<br />Replace with `setCallBackFilter()` |
| `scanTogether`                                         | 多个扫描任务一起执行                                         |

### 一个或多个扫描任务一起工作，Multiple scan tasks working together

### 第一种使用方式 The first way to use it

```kotlin
val scanFile1 = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
val scanFile2 = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
//独自执行扫描1 The first scan do alone
scanFile1.setCompleteCallBack{
     Log.d("ScanFile1","Complete")
}
scanFile1.startAsyncScan{
	//每扫描到一个文件，就回调这里 Scan to a file, call back here
	Log.d("ScanFile1","$it")
}
//独自执行扫描2 The second scan do alone
scanFile2.setCompleteCallBack{
     Log.d("ScanFile2","Complete")
}
scanFile2.startAsyncScan{
	//每扫描到一个文件，就回调这里 Scan to a file, call back here
	Log.d("scanFile2","$it")
}
```

### 第二种使用方式 The second way to use it

```kotlin
val scanFile1 = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
val scanFile2 = ScanFileUtil(ScanFileUtil.externalStorageDirectory)

//设置扫描1的回调 The first scan do alone
scanFile1.setScanningCallBack { file->   //不同于第一种使用 Different from the first use
	//每扫描到一个文件，就回调这里 Scan to a file, call back here
	Log.d("ScanFile1","$file")
}
scanFile1.setCompleteCallBack{
     Log.d("ScanFile1","ScanFile1 Complete")
}
scanFile1.startAsyncScan() //不同于第一种使用,使用此方式单独执行  Different from the first use，Use this method to execute alone

//设置扫描2的回调 The second scan do alone
scanFile2.setScanningCallBack { file->   //不同于第一种使用 Different from the first use
	//每扫描到一个文件，就回调这里 Scan to a file, call back here
	Log.d("ScanFile2","$file")
}
scanFile2.setCompleteCallBack{ 
     Log.d("ScanFile2","ScanFile2 Complete")
}
scanFile2.startAsyncScan()  //不同于第一种使用,使用此方式单独执行 Different from the first use，Use this method to execute alone


//======================================================
//or 或者同时执行 first and second begin together
//scanFile1.startAsyncScan() //不用调用这个方法 Do not call this method
//scanFile2.startAsyncScan() //不用调用这个方法 Do not call this method

//使用这个方法同时开启多个扫描任务 Use this method to start multiple scanning tasks at the same time
ScanFileUtil.scanTogether(scanFile1,scanFile2){
    //两个任务全部完成后在UI回调此方法
    //After both tasks are completed, call back this method in UI
    Toast.makeText(this, "ScanFile1 scanFile2 all completed", Toast.LENGTH_LONG).show()
}

注意：使用第一种方式扫描文件，不可以调用这个方法——‘scanTogether’
Be careful:The first way is not to call this method ——‘scanTogether’

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

