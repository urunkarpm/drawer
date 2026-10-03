package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.IconOverride

@Entity(tableName = "icon_pack_overrides")
data class IconPackOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "component_name")
    val componentName: String,
    @ColumnInfo(name = "icon_pack_package_name")
    val iconPackPackageName: String,
    @ColumnInfo(name = "drawable_name")
    val drawableName: String
) {
    fun toDomain(): IconOverride = IconOverride(
        componentName = componentName,
        iconPackPackageName = iconPackPackageName,
        drawableName = drawableName
    )

    companion object {
        fun fromDomain(override: IconOverride): IconPackOverrideEntity = IconPackOverrideEntity(
            componentName = override.componentName,
            iconPackPackageName = override.iconPackPackageName,
            drawableName = override.drawableName
        )
    }
}
