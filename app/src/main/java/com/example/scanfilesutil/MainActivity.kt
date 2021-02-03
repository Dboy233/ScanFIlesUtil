package com.example.scanfilesutil

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.scanfilesutil.utils.ScanFileUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    /**
     *第一个扫描任务
     */
    private val scanFileOne: ScanFileUtil = ScanFileUtil(ScanFileUtil.externalStorageDirectory)

    /**
     *第二个扫描任务
     */
    private val scanFileTwo: ScanFileUtil = ScanFileUtil(ScanFileUtil.externalStorageDirectory)


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

        val filter = ScanFileUtil.FileFilterBuilder().onlyScanDir().build()

        scanFileOne.setCallBackFilter(filter)

        scanFileOne.setScanFileListener(object : ScanFileUtil.ScanFileListener {
            var i = 0
            override fun onBegin() {
                oneFileList.clear()
            }

            override fun onComplete(timeConsuming: Long) {
                scan_info_tv.text = "complete time:$timeConsuming | file size = ${oneFileList.size}"
            }

            override fun onError() {

            }

            override fun onFile(file: File) {
                oneFileList.add(file)
                if (i <= 50) {
                    i++
                } else {
                    i = 0
                    scan_info_tv.text = file.canonicalPath
                }
            }
        })
    }

    /**
     * 第二个扫描任务
     */
    private fun initTwo() {

        val filter = ScanFileUtil.FileFilterBuilder().onlyScanFile().build()

        scanFileTwo.setCallBackFilter(filter)

        scanFileTwo.setScanFileListener(object : ScanFileUtil.ScanFileListenerAdapter() {
            var i = 0
            override fun onBegin() {
                twoFileList.clear()
            }

            override fun onComplete(timeConsuming: Long) {
                scan_two_info_tv.text =
                    "complete time:$timeConsuming | file size = ${twoFileList.size}"
            }

            override fun onFile(file: File) {
                twoFileList.add(file)
                if (i <= 50) {
                    i++
                } else {
                    i = 0
                    scan_two_info_tv.text = file.canonicalPath
                }
            }
        })
    }

    /**
     * 实例1 调用扫描模板1 使用startAsyncScan(callBack)回调
     */
    fun startOneScan(view: View) {
        scanFileOne.startScan()
    }

    /**
     * 实例2 调用扫描模板2 使用setScanningCallBack和startAsyncScan()完成扫描
     */
    fun startTwoScan(view: View) {
        scanFileTwo.startScan()
    }


    /**
     * 两个任务一起扫描
     */
    fun scanTogether(view: View) {
        scanFileOne.startScan()
        scanFileTwo.startScan()
    }

    /**
     * 停止所有扫描任务
     */
    fun stopScan(view: View?) {
        scanFileOne.stop()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopScan(null)
    }

}

