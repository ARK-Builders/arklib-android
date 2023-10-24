package dev.arkbuilders.arklib.data

import java.nio.file.Path

sealed class Message {
    class KindDetectFailed(val path: Path) : Message()
}