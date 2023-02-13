package com.saranomy.nolauncher

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoLauncher : ComponentActivity() {
    companion object {
        var self: NoLauncher? = null
        var apps by mutableStateOf(listOf<AppItem>())
        var icons = HashMap<String, ImageBitmap>()
    }

    private var queried by mutableStateOf(listOf<AppItem>())
    private var query by mutableStateOf("")
    private var loading by mutableStateOf(false)
    private var scrolling by mutableStateOf(false)
    private lateinit var appChangeReceiver: AppChangeReceiver
    private lateinit var focusManager: FocusManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            focusManager = LocalFocusManager.current
            val coroutineScope = rememberCoroutineScope()
            val lazyListState = rememberLazyListState()
            if (scrolling) {
                LaunchedEffect(coroutineScope) {
                    lazyListState.scrollToItem(0)
                }
                scrolling = false
            }
            NoLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TextField(
                            value = query,
                            onValueChange = {
                                query = it.replace("\n", "")
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
                            state = lazyListState,
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
                            if (apps.isNotEmpty() && query.isEmpty()) {
                                item {
                                    Text(
                                        text = "${stringResource(id = R.string.app_name)} ${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
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
        appChangeReceiver = AppChangeReceiver()
        registerReceiver(appChangeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        })
    }

    override fun onResume() {
        super.onResume()
        self = this
        load()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(appChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        self = null
    }

    override fun onBackPressed() {
        query = ""
        query()
        focusManager.clearFocus()
        scrolling = true
    }

    fun load(action: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            loading = true
            try {
                val temp = arrayListOf<AppItem>()
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                else packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val appHashCount = apps.sumOf {
                    it.packageName.hashCode()
                }
                val resultHashCount = result.sumOf {
                    if (packageManager.getLaunchIntentForPackage(it.packageName) != null) {
                        it.packageName.hashCode()
                    } else {
                        0
                    }
                }
                if (appHashCount != resultHashCount || apps.isEmpty() || (query.trim().isEmpty() && queried.isEmpty())) {
                    result.forEach { info ->
                        if (packageManager.getLaunchIntentForPackage(info.packageName) != null  || info.packageName == packageName) {
                            temp.add(
                                AppItem(
                                    name = info.loadLabel(packageManager).toString(),
                                    packageName = info.packageName,
                                )
                            )
                            if (!icons.containsKey(info.packageName)) {
                                icon(info.packageName, info.loadIcon(packageManager))
                            }
                        }
                    }
                    temp.sort()
                    apps = temp.toList()
                    query()
                }
            } catch (ignored: Exception) {
            }
            loading = false
        }
    }

    private fun query() {
        CoroutineScope(Dispatchers.IO).launch {
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
    }

    private fun icon(packageName: String, drawable: Drawable) {
        val bitmap = Bitmap.createBitmap(
            if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1,
            if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.apply {
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
        icons[packageName] = bitmap.asImageBitmap()
    }
}
