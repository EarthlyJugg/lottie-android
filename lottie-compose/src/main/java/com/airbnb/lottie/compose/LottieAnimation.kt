package com.airbnb.lottie.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.runtime.dispatch.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.LifecycleOwnerAmbient
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.getValue
import com.airbnb.lottie.*
import com.airbnb.lottie.utils.Logger
import java.io.FileInputStream
import java.util.zip.ZipInputStream

@Composable
fun LottieAnimation(
    spec: LottieAnimationSpec,
    animationState: LottieAnimationState = remember { LottieAnimationState(isPlaying = true) },
    modifier: Modifier = Modifier
) {
    val context = ContextAmbient.current
    var composition: LottieComposition? by remember { mutableStateOf(null) }
    onCommit(spec) {
        var isDisposed = false
        val task = when(spec) {
            is LottieAnimationSpec.RawRes -> LottieCompositionFactory.fromRawRes(context, spec.resId)
            is LottieAnimationSpec.Url -> LottieCompositionFactory.fromUrl(context, spec.url)
            is LottieAnimationSpec.File -> {
                val fis = FileInputStream(spec.fileName)
                when {
                    spec.fileName.endsWith("zip") -> LottieCompositionFactory.fromZipStream(ZipInputStream(fis), spec.fileName)
                    else -> LottieCompositionFactory.fromJsonInputStream(fis, spec.fileName)
                }
            }
            is LottieAnimationSpec.Asset -> LottieCompositionFactory.fromAsset(context, spec.assetName)
        }
        task.addListener { c ->
            if (!isDisposed) composition = c
        }.addFailureListener { e ->
            if (!isDisposed) Logger.error("Failed to parse composition.", e)
        }
        onDispose {
            isDisposed = true
        }
    }

    LottieAnimation(composition, animationState, modifier)
}

@Composable
fun LottieAnimation(
    composition: LottieComposition?,
    animationState: LottieAnimationState,
    modifier: Modifier = Modifier
) {
    val drawable = remember { LottieDrawable() }
    val isStarted by isStarted()
    val isPlaying = animationState.isPlaying && isStarted
    var progress by remember { mutableStateOf(0f) }


    onCommit(composition) {
        drawable.composition = composition
    }

    LaunchedTask(animationState.updateProgressChannel) {
        for (p in animationState.updateProgressChannel) {
            progress = p
            animationState.updateProgress(progress)
        }
    }

    LaunchedTask(composition, isPlaying, animationState.repeatCount) {
        if (!isPlaying || composition == null) return@LaunchedTask
        var repeatCount = 0
        if (isPlaying && progress == 1f) progress = 0f
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                val dTime = (frameTime - lastFrameTime) / TimeUnit.MILLISECONDS.toNanos(1).toFloat()
                lastFrameTime = frameTime
                val dProgress = dTime / composition.duration
                val previousProgress = progress
                progress = (progress + dProgress) % 1f
                if (previousProgress > progress) {
                    repeatCount++
                    if (repeatCount != 0 && repeatCount > animationState.repeatCount) {
                        progress = 1f
                        animationState.isPlaying = false
                    }
                }
                animationState.updateProgress(progress)
            }
        }
    }

    if (composition == null || composition.duration == 0f) return
    drawable.progress = progress

    Canvas(
        modifier = Modifier
            .maintainAspectRatio(composition)
            .then(modifier)
    ) {
        drawIntoCanvas { canvas ->
            withTransform({
                scale(size.width / composition.bounds.width().toFloat(), size.height / composition.bounds.height().toFloat(), Offset.Zero)
            }) {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}

@Composable
private fun isStarted(): State<Boolean> {
    val state = remember { mutableStateOf(false) }
    val lifecycleOwner = LifecycleOwnerAmbient.current
    onCommit(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                state.value = true
            }

            override fun onStop(owner: LifecycleOwner) {
                state.value = false
            }
        })
    }
    return state
}

@Composable
private fun Modifier.maintainAspectRatio(composition: LottieComposition?): Modifier {
    composition ?: return this
    return this.then(aspectRatio(composition.bounds.width() / composition.bounds.height().toFloat()))
}
