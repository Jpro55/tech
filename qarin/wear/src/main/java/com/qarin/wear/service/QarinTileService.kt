package com.qarin.wear.service

import android.content.Intent
import androidx.wear.tiles.*
import androidx.wear.tiles.material.Button
import androidx.wear.tiles.material.ButtonColors
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.qarin.wear.ui.MainActivity

class QarinTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(buildLayout())
            )
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build()
        )
    }

    private fun buildLayout(): LayoutElementBuilders.LayoutElement {
        val clickable = ModifiersBuilders.Clickable.Builder()
            .setId("open_qarin")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setClassName(MainActivity::class.java.name)
                            .setPackageName(packageName)
                            .build()
                    )
                    .build()
            )
            .build()

        return PrimaryLayout.Builder(deviceParameters())
            .setPrimaryLabelTextContent(
                Text.Builder(this, "قرين")
                    .setTypography(Typography.TYPOGRAPHY_TITLE3)
                    .setColor(ColorBuilders.argb(0xFF6200EE.toInt()))
                    .build()
            )
            .setContent(
                Button.Builder(this, clickable)
                    .setTextContent("افتح")
                    .setButtonColors(ButtonColors.primaryButtonColors(ColorBuilders.argb(0xFF6200EE.toInt())))
                    .build()
            )
            .build()
    }

    private fun deviceParameters(): DeviceParametersBuilders.DeviceParameters {
        val metrics = resources.displayMetrics
        return DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp((metrics.widthPixels / metrics.density).toInt())
            .setScreenHeightDp((metrics.heightPixels / metrics.density).toInt())
            .setScreenDensity(metrics.density)
            .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
            .build()
    }
}
