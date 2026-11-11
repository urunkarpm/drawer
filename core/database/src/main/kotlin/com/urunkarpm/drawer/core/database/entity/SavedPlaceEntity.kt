package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.SavedPlace

@Entity(tableName = "saved_place")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "radius_m")
    val radiusM: Float = 200f
) {
    fun toDomain(): SavedPlace = SavedPlace(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusM = radiusM
    )

    companion object {
        fun fromDomain(place: SavedPlace): SavedPlaceEntity = SavedPlaceEntity(
            id = place.id,
            name = place.name,
            latitude = place.latitude,
            longitude = place.longitude,
            radiusM = place.radiusM
        )
    }
}
