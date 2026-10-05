package com.urunkarpm.drawer.feature.home.dragdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.urunkarpm.drawer.core.designsystem.modifier.appDragSource as coreAppDragSource
import com.urunkarpm.drawer.core.model.AppInfo

class AppDragDropState {
    var isDragging by mutableStateOf(false)
        private set
    var draggingApp by mutableStateOf<AppInfo?>(null)
        private set
    var dragPosition by mutableStateOf(Offset.Zero)
        private set
    var sourceGroupId by mutableStateOf<String?>(null)
        private set

    var dockBounds by mutableStateOf<Rect?>(null)
    val groupBounds = mutableStateMapOf<String, Rect>()
    // groupId -> list of app center Y coordinates or just bounding rects
    val appBounds = mutableStateMapOf<String, MutableMap<String, Rect>>()

    fun registerAppBounds(groupId: String, key: String, rect: Rect) {
        val map = appBounds.getOrPut(groupId) { mutableMapOf() }
        map[key] = rect
    }

    fun isOverDock(pos: Offset = dragPosition): Boolean {
        val bounds = dockBounds
        if (bounds != null) {
            val horizontalMatch = pos.x in (bounds.left - 48f)..(bounds.right + 48f)
            val verticalMatch = pos.y >= (bounds.top - 60f)
            return horizontalMatch && verticalMatch
        }
        return false
    }

    fun hoveredGroupId(pos: Offset = dragPosition): String? {
        return groupBounds.entries.firstOrNull { it.value.inflate(36f).contains(pos) }?.key
    }
    
    fun calculateTargetIndex(groupId: String, pos: Offset): Int {
        val boundsMap = appBounds[groupId]
        if (boundsMap != null && boundsMap.isNotEmpty()) {
            var closestIndex = 0
            var closestDistance = Float.MAX_VALUE
            var index = 0
            for ((_, rect) in boundsMap.entries) {
                val center = rect.center
                val dist = (pos - center).getDistanceSquared()
                if (dist < closestDistance) {
                    closestDistance = dist
                    closestIndex = if (pos.x > center.x || pos.y > rect.bottom - 8f) {
                        index + 1
                    } else {
                        index
                    }
                }
                index++
            }
            return closestIndex.coerceIn(0, boundsMap.size)
        }
        
        // Pure math fallback (assumes 4 columns grid view, with ~48dp header)
        val bounds = groupBounds[groupId] ?: return 0
        val relativeX = (pos.x - bounds.left).coerceAtLeast(0f)
        val relativeY = (pos.y - bounds.top).coerceAtLeast(0f)
        if (relativeY < 120f) return 0
        
        val rowHeight = 220f
        val row = ((relativeY - 120f) / rowHeight).toInt().coerceAtLeast(0)
        val colWidth = (bounds.width / 4f).coerceAtLeast(1f)
        val col = (relativeX / colWidth).toInt().coerceIn(0, 3)
        return row * 4 + col
    }

    fun startDrag(app: AppInfo, rootPosition: Offset, sourceGroupId: String? = null) {
        this.draggingApp = app
        this.dragPosition = rootPosition
        this.sourceGroupId = sourceGroupId
        this.isDragging = true
    }

    fun updateDrag(dragAmount: Offset) {
        this.dragPosition += dragAmount
    }

    fun endDrag(): DragDropResult {
        val app = draggingApp
        val pos = dragPosition
        val srcGroup = sourceGroupId
        isDragging = false
        draggingApp = null
        sourceGroupId = null

        if (app == null) return DragDropResult.None

        if (isOverDock(pos)) {
            val bounds = dockBounds
            var targetIndex = 0
            if (bounds != null) {
                val dockWidth = bounds.width
                val numItems = 4 // approx
                val itemWidth = dockWidth / numItems
                val dropX = pos.x - bounds.left
                targetIndex = (dropX / itemWidth).toInt().coerceIn(0, numItems)
            }
            return DragDropResult.DroppedOnDock(app, targetIndex, srcGroup)
        }
        val targetGroup = hoveredGroupId(pos)
        if (targetGroup != null) {
            val targetIndex = calculateTargetIndex(targetGroup, pos)
            return DragDropResult.DroppedOnGroup(app, targetGroup, targetIndex, srcGroup)
        }
        return DragDropResult.DroppedOnHome(app, srcGroup)
    }

    fun cancelDrag() {
        isDragging = false
        draggingApp = null
        sourceGroupId = null
    }
}

sealed interface DragDropResult {
    data object None : DragDropResult
    data class DroppedOnDock(val app: AppInfo, val targetIndex: Int, val sourceGroupId: String?) : DragDropResult
    data class DroppedOnGroup(val app: AppInfo, val targetGroupId: String, val targetIndex: Int, val sourceGroupId: String?) : DragDropResult
    data class DroppedOnHome(val app: AppInfo, val sourceGroupId: String?) : DragDropResult
}

@Composable
fun rememberAppDragDropState(): AppDragDropState {
    return remember { AppDragDropState() }
}

fun Modifier.appDragSource(
    app: AppInfo,
    onAppClick: () -> Unit,
    onAppLongClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (isDropped: Boolean) -> Unit
): Modifier = this.coreAppDragSource(
    key = app.componentKey,
    onAppClick = onAppClick,
    onAppLongClick = onAppLongClick,
    onDragStart = onDragStart,
    onDrag = onDrag,
    onDragEnd = onDragEnd
)

