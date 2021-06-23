package com.zaihui.installplugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileNotFoundException

/**
 * 1 获取Registrar 这个接口可以获取 context
 * 2 添加自身所需依赖
 * @property registrar Registrar
 * @constructor
 */
public class InstallPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mContext: Context

        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            this.mContext = registrar.activity()
            val channel = MethodChannel(registrar.messenger(), "install_plugin")
            channel.setMethodCallHandler(InstallPlugin())
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "installApk" -> {
                val filePath = call.argument<String>("filePath")
                val appId = call.argument<String>("appId")
                Log.d("android plugin", "installApk $filePath $appId")
                try {
                    installApk(mContext, filePath, appId)
                    result.success("Success")
                } catch (e: Throwable) {
                    result.error(e.javaClass.simpleName, e.message, null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun installApk(context: Context, filePath: String?, currentAppId: String?) {
        if (filePath == null) throw NullPointerException("fillPath is null!")
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException("$filePath is not exist! or check permission")
        val intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //7.0及以上
            val contentUri = FileProvider.getUriForFile(context, "$currentAppId.fileProvider.install", file)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
            context.startActivity(intent)
        } else {
            //7.0以下
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "install_plugin")
        channel.setMethodCallHandler(InstallPlugin())
    }

    override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {

    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mContext = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(p0: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
    }
}
