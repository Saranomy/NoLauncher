package com.saranomy.nolauncher

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        var self: MainActivity? = null
        var apps by mutableStateOf(listOf<AppItem>())
    }

    private var queried by mutableStateOf(listOf<AppItem>())
    private var query by mutableStateOf("")
    private var loading by mutableStateOf(false)
    private var appChangeReceiver = AppChangeReceiver()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TextField(
                            value = query,
                            onValueChange = {
                                query = it
                                query()
                            },
                            placeholder = {
                                Text(text = stringResource(id = R.string.search_by_name))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        query = ""
                                        query()
                                    }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent
                            )
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = queried,
                                key = {
                                    it.packageName
                                }
                            ) {
                                it.Display(onClick = {
                                    try {
                                        startActivity(packageManager.getLaunchIntentForPackage(it.packageName))
                                    } catch (ignored: Exception) {
                                    }
                                }, onLongClick = {
                                    try {
                                        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${it.packageName}")))
                                    } catch (ignored: Exception) {
                                    }
                                })
                            }
                            if (apps.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "${stringResource(id = R.string.app_name)} ${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                try {
                                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                                                    })
                                                } catch (e: Exception) {
                                                    startActivity(
                                                        Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse("http://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                                                        )
                                                    )
                                                }
                                            }
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (loading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        Log.wtf("TAG", "onCreate")
        registerReceiver(appChangeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.wtf("TAG", "onConfigurationChanged")
    }

    override fun onResume() {
        super.onResume()
        self = this
        if (apps.isEmpty()) load()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(appChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.wtf("TAG", "onStop")
        self = null
    }

    override fun onBackPressed() {
        query = ""
    }

    fun load() {
        Log.wtf("TAG", "load")
        CoroutineScope(Dispatchers.IO).launch {
            loading = true
            try {
                val temp = arrayListOf<AppItem>()
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                else packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                result.forEach { info ->
                    if (packageManager.getLaunchIntentForPackage(info.packageName) != null) {
                        temp.add(
                            AppItem(
                                name = info.loadLabel(packageManager).toString(),
                                packageName = info.packageName,
                                icon = icon(info.loadIcon(packageManager))
                            )
                        )
                    }
                }
                temp.sort()
                apps = temp.toList()
                query()
            } catch (ignored: Exception) {
            }
            loading = false
        }
    }

    private fun query() {
        Log.wtf("TAG", "query $query")
        val temp = arrayListOf<AppItem>()
        val matchQuery = query.trim()
        queried = if (matchQuery.isEmpty()) {
            apps.toList()
        } else {
            apps.forEach {
                if (it.name.contains(matchQuery, true)) {
                    temp.add(it)
                }
            }
            temp.sort()
            temp
        }
    }

    private fun icon(drawable: Drawable): ImageBitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.apply {
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
        return bitmap.asImageBitmap()
    }
}