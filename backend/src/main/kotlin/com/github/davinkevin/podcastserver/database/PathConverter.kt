package com.github.davinkevin.podcastserver.database

import org.jooq.Converter
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Created by kevin on 18/06/2022
 */
private val innerConverter = Converter.ofNullable(String::class.java, Path::class.java, ::Path, Path::toString)
class PathConverter: Converter<String, Path> by innerConverter
