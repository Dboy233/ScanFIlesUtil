package com.example.scanfilesutil

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scanfilesutil.utils.ScanFileUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*


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


//        val path = Environment.getExternalStorageDirectory().absolutePath

//        val path = externalCacheDir!!.absolutePath

        val scanFile = ScanFileUtil(ScanFileUtil.externalStorageDirectory)
            .apply {
                setFilter(
                    ScanFileUtil.FileFilterBuilder().apply {

                    }.build()
                )
            }

        scanFile.completeCallBack {
            Toast.makeText(this, "扫描完成", Toast.LENGTH_LONG).show()
        }

        scanBtn.setOnClickListener {
            scanFile.startAsyncScan {
                Log.d("Scan=>>", "=> ${it.name}   是否是隐藏-${it.isHidden}  读${it.canRead()} 写 ${it.canWrite()}")
            }
        }

        stopBtn.setOnClickListener {
            scanFile.stop()
        }
    }
}
