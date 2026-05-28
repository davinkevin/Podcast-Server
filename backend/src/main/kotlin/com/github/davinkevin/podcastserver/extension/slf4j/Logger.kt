package com.github.davinkevin.podcastserver.extension.slf4j

import org.slf4j.Logger

fun Logger.errorWithDebugStack(message: String, vararg args: Any?, throwable: Throwable?) {
    if (throwable != null && isDebugEnabled) error(message, *args, throwable)
    else error(message, *args)
}
