package com.example.scanfilesutil

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scanfilesutil.utils.FileUtils
import com.example.scanfilesutil.utils.ScanFileUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FilenameFilter


class MainActivity : AppCompatActivity() {


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


        val scanFile = ScanFileUtil(ScanFileUtil.externalStorageDirectory)

        scanFile.apply {
            setScanLevel(2)
            setCallBackFilter(ScanFileUtil.FileFilterBuilder().apply {
                onlyScanDir()
                addCustomFilter(FilenameFilter { dir, name ->
                    FileUtils.getDirLength(dir) == 0L
                })
            }.build())
        }

        scanFile.completeCallBack {
            Toast.makeText(this, "scan end 扫描完成", Toast.LENGTH_LONG).show()
        }

        scanBtn.setOnClickListener {
            scanFile.startAsyncScan {
                Log.d(
                    "Scan", "${it.absolutePath}  size ${FileUtils.getDirLength(it)}  "
                )
            }
        }

        stopBtn.setOnClickListener {
            scanFile.stop()
        }
    }
}
