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

    var dockBounds: Rect? = null
    val groupBounds = mutableMapOf<String, Rect>()
    // groupId -> list of app center Y coordinates or just bounding rects
    val appBounds = mutableMapOf<String, MutableMap<String, Rect>>()

    fun registerAppBounds(groupId: String, key: String, rect: Rect) {
        val map = appBounds.getOrPut(groupId) { mutableMapOf() }
        map[key] = rect
    }

    fun isOverDock(pos: Offset = dragPosition): Boolean {
        if (!isDragging) return false
        val bounds = dockBounds ?: return false
        val horizontalMatch = pos.x in (bounds.left - 48f)..(bounds.right + 48f)
        val verticalMatch = pos.y >= (bounds.top - 60f)
        return horizontalMatch && verticalMatch
    }

    fun hoveredGroupId(pos: Offset = dragPosition): String? {
        if (!isDragging) return null
        val exact = groupBounds.entries.firstOrNull { it.value.contains(pos) }?.key
        if (exact != null) return exact
        return groupBounds.entries.firstOrNull { it.value.inflate(36f).contains(pos) }?.key
    }
    
    fun calculateTargetIndex(groupId: String, pos: Offset): Int {
        val boundsMap = appBounds[groupId]
        if (boundsMap != null && boundsMap.isNotEmpty()) {
            // Sort bounding rects in visual grid order: row-by-row (top-to-bottom), then left-to-right
            // ponytail: ceiling: Hardcoded 10px tolerance for row alignment and pixel math for hitboxes won't scale flawlessly to all densities/foldables. Upgrade path: Inject LocalDensity and convert fixed sizes from dp to px.
            val sortedRects = boundsMap.values.sortedWith(
                compareBy<Rect> { it.top.toInt() / 10 }
                    .thenBy { it.center.x }
            )
            val total = sortedRects.size

            // If dropped past the end of the items
            val lastRect = sortedRects.last()
            if (pos.y > lastRect.bottom || (pos.y >= lastRect.top - 20f && pos.x > lastRect.right)) {
                return total
            }

            var closestIndex = 0
            var closestDistance = Float.MAX_VALUE
            for ((index, rect) in sortedRects.withIndex()) {
                val dist = (pos - rect.center).getDistanceSquared()
                if (dist < closestDistance) {
                    closestDistance = dist
                    closestIndex = index
                }
            }
            return closestIndex.coerceIn(0, total)
        }
        
        // Pure math fallback (assumes 2 columns grid view with ~48dp header)
        val bounds = groupBounds[groupId] ?: return 0
        val relativeX = (pos.x - bounds.left).coerceAtLeast(0f)
        val relativeY = (pos.y - bounds.top).coerceAtLeast(0f)
        if (relativeY < 120f) return 0
        
        val rowHeight = 200f
        val row = ((relativeY - 120f) / rowHeight).toInt().coerceAtLeast(0)
        val colWidth = (bounds.width / 2f).coerceAtLeast(1f)
        val col = (relativeX / colWidth).toInt().coerceIn(0, 1)
        return row * 2 + col
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

        if (app == null) {
            cancelDrag()
            return DragDropResult.None
        }

        val targetGroup = hoveredGroupId(pos)
        val result = when {
            isOverDock(pos) -> {
                val bounds = dockBounds
                var targetIndex = 0
                if (bounds != null) {
                    val dockWidth = bounds.width
                    val numItems = 4 // approx
                    val itemWidth = dockWidth / numItems
                    val dropX = pos.x - bounds.left
                    targetIndex = (dropX / itemWidth).toInt().coerceIn(0, numItems)
                }
                DragDropResult.DroppedOnDock(app, targetIndex, srcGroup)
            }
            targetGroup != null -> {
                val targetIndex = calculateTargetIndex(targetGroup, pos)
                DragDropResult.DroppedOnGroup(app, targetGroup, targetIndex, srcGroup)
            }
            else -> DragDropResult.DroppedOnHome(app, srcGroup)
        }

        isDragging = false
        draggingApp = null
        sourceGroupId = null
        return result
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
    onDragEnd: (isDropped: Boolean) -> Unit,
    longPressRequired: Boolean = true
): Modifier = this.coreAppDragSource(
    key = app.componentKey,
    onAppClick = onAppClick,
    onAppLongClick = onAppLongClick,
    onDragStart = onDragStart,
    onDrag = onDrag,
    onDragEnd = onDragEnd,
    longPressRequired = longPressRequired
)

