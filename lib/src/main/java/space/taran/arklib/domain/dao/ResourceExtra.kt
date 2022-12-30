package space.taran.arklib.domain.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.TypeConverters
import space.taran.arklib.Converters
import space.taran.arklib.ResourceId

@Entity(
    primaryKeys = ["resource", "ordinal"],
    foreignKeys = [
        ForeignKey(
            entity = Resource::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("resource"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ResourceExtra(
    @ColumnInfo(index = true)
    @TypeConverters(Converters::class)
    val resource: ResourceId,

    val ordinal: Int,

    val value: String
)
