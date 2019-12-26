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
        val scanFile2 = ScanFileUtil(ScanFileUtil.externalStorageDirectory)

        scanFile.completeCallBack {
//            Toast.makeText(this, "1扫描完成", Toast.LENGTH_LONG).show()
            Log.d("Scan","1扫描完成")
        }
        scanFile2.completeCallBack {
//            Toast.makeText(this, "2扫描完成", Toast.LENGTH_LONG).show()
            Log.d("Scan","2扫描完成")

        }

        scanBtn.setOnClickListener {
            scanFile.startAsyncScan {
                Log.d(
                    "Scan=>>",
                    "=>1 ${it.name}   "
                )
            }
            scanFile2.startAsyncScan {
                Log.d(
                    "Scan=>>",
                    "=>2 ${it.name}   "
                )
            }
            ScanFileUtil.await(scanFile.getAwaitInstance(), scanFile2.getAwaitInstance()) {
                Log.d("Scan", "全部完成了")
            }
        }

        stopBtn.setOnClickListener {
            scanFile.stop()
        }
    }
}
