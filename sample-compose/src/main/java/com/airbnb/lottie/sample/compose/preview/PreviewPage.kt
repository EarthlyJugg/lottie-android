package com.airbnb.lottie.sample.compose.preview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.ui.tooling.preview.Preview
import com.airbnb.lottie.sample.compose.ComposeFragment
import com.airbnb.lottie.sample.compose.R
import com.airbnb.lottie.sample.compose.composables.Marquee
import com.airbnb.lottie.sample.compose.player.PlayerFragment
import com.airbnb.lottie.sample.compose.ui.LottieTheme
import com.airbnb.lottie.sample.compose.utils.findNavController
import com.airbnb.mvrx.asMavericksArgs

class PreviewFragment : ComposeFragment() {
    @Composable
    override fun root() {
        PreviewPage()
    }
}

@Composable
fun PreviewPage() {
    var showingAssetsDialog by remember { mutableStateOf(false) }
    val navController = findNavController()

    Column {
        Marquee(stringResource(R.string.tab_preview))
        PreviewRow(R.drawable.ic_qr_scan, R.string.scan_qr_code) {

        }
        PreviewRow(R.drawable.ic_file, R.string.open_file) {

        }
        PreviewRow(R.drawable.ic_network, R.string.enter_url) {

        }
        PreviewRow(R.drawable.ic_storage, R.string.load_from_assets) {
            showingAssetsDialog = true
        }
    }

    AssetsDialog(
        showingAssetsDialog,
        onDismiss = { showingAssetsDialog = false },
        onAssetSelected = { assetName ->
            val args = PlayerFragment.Args.Asset(assetName)
            navController.navigate(R.id.player, args.asMavericksArgs())
        }
    )
}

@Composable
private fun PreviewRow(
    @DrawableRes iconRes: Int,
    @StringRes textRes: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .preferredHeight(48.dp)
            ) {
                Icon(
                    vectorResource(iconRes),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(16.dp)
                )
                Text(
                    stringResource(textRes),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            }
            Divider(color = Color.LightGray)
        }
    }
}

@Composable
fun AssetsDialog(isShowing: Boolean, onDismiss: () -> Unit, onAssetSelected: (assetName: String) -> Unit) {
    if (!isShowing) return
    val context = ContextAmbient.current
    val assets = context.assets.list("")
        ?.asSequence()
        ?.filter { it.endsWith(".json") || it.endsWith(".zip") }
        ?.toList()
        ?: emptyList()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp)
        ) {
            Column {
                assets.forEach { asset ->
                    AssetRow(asset, onClick = {
                        onDismiss()
                        onAssetSelected(asset)
                    })
                }
            }
        }
    }
}

@Composable
private fun AssetRow(name: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(name)
    }
}

@Preview
@Composable
fun PreviewPagePreview() {
    LottieTheme {
        Box(modifier = Modifier.background(Color.White)) {
            PreviewPage()
        }
    }
}
