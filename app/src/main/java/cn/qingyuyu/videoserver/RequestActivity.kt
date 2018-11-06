package cn.qingyuyu.videoserver

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import androidx.core.app.ActivityCompat.requestPermissions
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.os.Build
import android.app.Activity
import android.content.Intent
import android.util.Log


class RequestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)
        requestPermissions()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)// 获取到权限，作相应处理
        {
            startActivity(Intent(this@RequestActivity,MainActivity::class.java))
        }
    }

    fun requestPermissions() {
        var isPermission=false
        if (Build.VERSION.SDK_INT >= 23 && !isPermission) {
            val permission = arrayOf<String>(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {//检查权限
                Log.e("请求权限", "正在请求")
                requestPermissions(permission, 0)//请求
            }
            else
                isPermission=true
        } else {
            isPermission = true
        }

        if (isPermission) {
           startActivity(Intent(this@RequestActivity,MainActivity::class.java))
        }
    }
}
