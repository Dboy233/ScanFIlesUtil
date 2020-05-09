package com.example.scanfilesutil

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scanfilesutil.utils.FileUtils
import com.example.scanfilesutil.utils.ScanFileUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilenameFilter

class MainActivity : AppCompatActivity() {
    /**
     *第一个扫描任务
     */
    private lateinit var scanFileOne: ScanFileUtil

    /**
     *第二个扫描任务
     */
    private lateinit var scanFileTwo: ScanFileUtil

    /**
     * 同时扫描管理类
     */
    var mScanTogetherManager: ScanFileUtil.ScanTogetherManager = ScanFileUtil.ScanTogetherManager()


    private val oneFileList = mutableListOf<File>()
    private val twoFileList = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RxPermissions(this)
            .request(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.GET_PACKAGE_SIZE
            )
            .subscribe()

        initOne()
        initTwo()
    }

    /**
     * 第一个扫描任务
     */
    private fun initOne() {
        scanFileOne = ScanFileUtil(ScanFileUtil.externalStorageDirectory)

//        scanFileOne.apply {
//            setCallBackFilter(ScanFileUtil.FileFilterBuilder().apply {
//                onlyScanDir()
//                addCustomFilter(FilenameFilter { dir, name ->
//                    FileUtils.getDirLength(dir) == 0L
//                })
//            }.build())
//
//        }

        scanFileOne.setScanFileListener(object : ScanFileUtil.ScanFileListener {
            var i = 0
            override fun scanBegin() {
                i = 0
            }
            override fun scanComplete(timeConsuming: Long) {
                //   处理你的扫描结果 Process your scan results
                //   Log.d("tow Scan",oneFileList.toString())
                scan_info_tv.text =
                    " 扫描任务1完成 one scan complete ; time:${timeConsuming}"
//            Toast.makeText(this, "one scan end 扫描完成", Toast.LENGTH_SHORT).show()
            }
            override suspend fun scanningCallBack(file: File) {

                oneFileList.add(file)//保存扫描数据 Save scan data
                if (i >= 20) {
                    i = 0
                    //20次回调一次，减少页面刷新频次
                    withContext(Dispatchers.Main) {
                        scan_info_tv.text = file.absolutePath//展示过程 Show the process
                    }
                }
                i++
                Log.d(
                    "one Scan", "${file.absolutePath}  size ${FileUtils.getDirLength(file)}  "
                )
            }

        })
    }

    /**
     * 第二个扫描任务
     */
    private fun initTwo() {

        scanFileTwo = ScanFileUtil(ScanFileUtil.externalStorageDirectory,
            object : ScanFileUtil.ScanFileListener {
                var i = 0
                override fun scanBegin() {
                    i = 0
                }

                override fun scanComplete(timeConsuming: Long) {
                    //   处理你的扫描结果 Process your scan results
                    //   Log.d("tow Scan",twoFileList.toString())
                    scan_two_info_tv.text =
                        " 扫描任务2完成 tow scan complete ;time:${timeConsuming}"
                    // Toast.makeText(this, "two scan end 扫描完成", Toast.LENGTH_SHORT).show()
                }

                override suspend fun scanningCallBack(file: File) {
                    twoFileList.add(file)//保存扫描数据 Save scan data
                    if (i >= 20) {
                        i = 0
                        withContext(Dispatchers.Main) {
                            scan_two_info_tv.text = file.absolutePath //展示过程 Show the process
                        }
                    }
                    i++
                    Log.d(
                        "two Scan", "${file.absolutePath}  size ${FileUtils.getFileLength(file)}  "
                    )
                }
            })

        //设置过滤规则
        scanFileTwo.setCallBackFilter(ScanFileUtil.FileFilterBuilder()
            .apply {
                onlyScanFile()
                scanApkFiles()
            }
            .build())
    }

    /**
     * 实例1 调用扫描模板1 使用startAsyncScan(callBack)回调
     */
    fun startOneScan(view: View) {
        oneFileList.clear()
        var i = 0
        //开始扫描
        scanFileOne.startAsyncScan()
    }

    /**
     * 实例2 调用扫描模板2 使用setScanningCallBack和startAsyncScan()完成扫描
     */
    fun startTwoScan(view: View) {
        twoFileList.clear()
        //开始扫描
        scanFileTwo.startAsyncScan()
    }


    /**
     * 两个任务一次扫描
     */
    fun scanTogether(view: View) {
        oneFileList.clear()
        twoFileList.clear()
        mScanTogetherManager.scan(scanFileOne, scanFileTwo) {
            Log.d("Scan", "one scan and two scan end,扫描1 和 扫描2 完成")
            Toast.makeText(
                applicationContext,
                "one scan and two scan end,扫描1 和 扫描2 完成",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 停止所有扫描任务
     */
    fun stopScan(view: View) {
        mScanTogetherManager.cancel()
        scanFileOne.stop()
        scanFileTwo.stop()
    }

}

