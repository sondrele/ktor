/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io


/**
 * Reads a line of UTF-8 characters up to [limit] characters.
 * Supports both CR-LF and LF line endings.
 * Throws an exception if the specified [limit] has been exceeded.
 *
 * @return a line string with no line endings or `null` if channel has been closed
 * and no characters were read.
 */
public suspend fun ByteReadChannel.readUTF8Line(limit: Int = Int.MAX_VALUE): String? {
    val line = buildString {
        if (!readUTF8LineTo(this, limit)) return null
    }

    return line
}

public suspend fun <A : Appendable> ByteReadChannel.readUTF8LineTo(out: A, limit: Int = Int.MAX_VALUE): Boolean {
    TODO()
}
