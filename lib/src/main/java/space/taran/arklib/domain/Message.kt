package space.taran.arklib.domain

import java.nio.file.Path

sealed class Message {
    class KindDetectFailed(val path: Path) : Message()
}