package com.github.davinkevin.podcastserver.rss

import org.jdom2.Element

fun rootRss(channel: Element): Element = Element("rss").apply {
    addContent(channel)
    addNamespaceDeclaration(itunesNS)
}