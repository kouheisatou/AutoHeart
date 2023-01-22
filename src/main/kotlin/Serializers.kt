import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MutableStateFlowOfIntsSerializer :
    KSerializer<MutableStateFlow<Int?>> by MutableStateFlowSerializer(Int.serializer())

object MutableStateFlowOfAreasSerializer :
    KSerializer<MutableStateFlow<Area?>> by MutableStateFlowSerializer(Area.serializer())
object MutableStateListOfAreasSerializer :
    KSerializer<SnapshotStateList<Area>> by MutableStateListSerializer(Area.serializer())
object MutableStateStringIntMapSerializer :
    KSerializer<SnapshotStateMap<String, Int>> by MutableStateMapSerializer()

private class MutableStateFlowSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<MutableStateFlow<T?>> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): MutableStateFlow<T?> {
        return if(!decoder.decodeNotNullMark()){
            MutableStateFlow(null)
        }else{
            MutableStateFlow(dataSerializer.deserialize(decoder))
        }
    }

    override fun serialize(encoder: Encoder, value: MutableStateFlow<T?>) {
        if (value.value != null) {
            dataSerializer.serialize(encoder, value.value!!)
        } else {
            encoder.encodeNull()
        }
    }
}

private class MutableStateListSerializer<T>(dataSerializer: KSerializer<T>) : KSerializer<SnapshotStateList<T>> {
    private val listSerializer = ListSerializer(dataSerializer)
    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): SnapshotStateList<T> {
        return mutableStateListOf<T>().apply {
            this.addAll(listSerializer.deserialize(decoder))
        }
    }

    override fun serialize(encoder: Encoder, value: SnapshotStateList<T>) {
        listSerializer.serialize(encoder, value)
    }

}

private class MutableStateMapSerializer : KSerializer<SnapshotStateMap<String, Int>> {
    private val mapSerializer = MapSerializer(String.serializer(), Int.serializer())
    override val descriptor: SerialDescriptor = mapSerializer.descriptor
    override fun deserialize(decoder: Decoder): SnapshotStateMap<String, Int> {
        return mutableStateMapOf<String, Int>().apply {
            mapSerializer.deserialize(decoder).forEach{
                this[it.key] = it.value
            }
        }
    }

    override fun serialize(encoder: Encoder, value: SnapshotStateMap<String, Int>) {
        mapSerializer.serialize(encoder, value)
    }

}