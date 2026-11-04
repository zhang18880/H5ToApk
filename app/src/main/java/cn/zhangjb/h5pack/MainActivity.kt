package cn.zhangjb.h5pack
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
class MainActivity : AppCompatActivity() {
    private var webView: WebView? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalSystemUiVisibility = 0
    // 新增：保存进入全屏前的屏幕方向
    private var originalOrientation = 0

    // 文件选择器回调契约（新版，替代废弃onActivityResult）
    private val fileChooserLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris: Array<Uri>? = if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data!!
                data.clipData?.run {
                    val list = mutableListOf<Uri>()
                    for (i in 0 until itemCount) {
                        list.add(getItemAt(i).uri)
                    }
                    list.toTypedArray()
                } ?: data.data?.run { arrayOf(this) }
            } else null
            fileCallback?.onReceiveValue(uris)
            fileCallback = null
        }
    // 动态权限申请契约：定位、相机、麦克风
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // 权限申请结果交给WebChromeClient内部权限回调自动处理
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        val wv = webView!!
        val settings: WebSettings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        // 自动播放音视频
        settings.mediaPlaybackRequiresUserGesture = false
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        wv.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(view: WebView?, url: String): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(Uri.parse(url))
            }
            //scheme 拦截 支出只放行了 微信和支付宝
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                // 白名单：微信、支付宝协议放行
                if(url.startsWith("weixin://") || url.startsWith("alipay://")){
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, request.url)
                        startActivity(intent)
                        true
                    }catch (e:Exception){
                        // 没有安装微信/支付宝，直接阻止，不报错
                        true
                    }
                }
                // 非http/https 其他app唤起全部干掉
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        // ========= WebChromeClient：文件上传 + 摄像头/麦克风/定位权限 =========
        wv.webChromeClient = object : WebChromeClient() {

            // 视频进入全屏
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                super.onShowCustomView(view, callback)
                if (customView != null) {
                    onHideCustomView()
                    return
                }
                customView = view
                customViewCallback = callback
                originalSystemUiVisibility = window.decorView.systemUiVisibility
                // 保存进入全屏之前屏幕方向
                originalOrientation = requestedOrientation
                // 设置传感器横屏，支持左右两个方向横屏播放
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

                val decor = window.decorView as ViewGroup
                decor.addView(customView)
                wv.visibility = View.GONE
            }

            // 视频退出全屏
            override fun onHideCustomView() {
                super.onHideCustomView()
                if (customView == null) return
                val decor = window.decorView as ViewGroup
                decor.removeView(customView)
                customViewCallback?.onCustomViewHidden()
                customView = null
                customViewCallback = null
                window.decorView.systemUiVisibility = originalSystemUiVisibility
                wv.visibility = View.VISIBLE
                // 恢复原来的屏幕方向
                requestedOrientation = originalOrientation
            }

            // H5 文件上传 input type="file"
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback = filePathCallback
                val intent = fileChooserParams?.createIntent()
                intent?.type = "*/*" // 支持全部文件：pdf word ppt txt
                fileChooserLauncher.launch(Intent.createChooser(intent, "选择文件"))
                return true
            }
            // H5请求摄像头/麦克风权限回调
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.let {
                    permissionLauncher.launch(it.resources)
                }
            }
            // H5定位请求
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                callback?.invoke(origin, true, false)
            }
        }
        //wv.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        wv.loadUrl("https://ulsc.net:60110/ncat/")
        setContentView(wv)
    }
    override fun onDestroy() {
        webView?.stopLoading()
        webView?.webChromeClient = null
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
