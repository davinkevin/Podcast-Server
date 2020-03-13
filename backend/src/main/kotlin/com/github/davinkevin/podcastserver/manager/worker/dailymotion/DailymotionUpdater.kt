package com.github.davinkevin.podcastserver.manager.worker.dailymotion


/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
//@Component
//class DailymotionUpdater(val signatureService: SignatureService, val jsonService: JsonService, val imageService: ImageService) : Updater {
//
//    override fun blockingFindItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> {
//        return USER_NAME_EXTRACTOR
//                .on(podcast.url.toASCIIString()).group(1)
//                .toOption()
//                .map { API_LIST_OF_ITEMS.format(it) }
//                .flatMap { jsonService.parseUrl(it) }
//                .map { it.read("list", LIST_DAILYMOTION_VIDEO_DETAIL_TYPE) }
//                .getOrElse { setOf() }
//                .map { ItemFromUpdate(
//                        url = URI(ITEM_URL.format(it.id)),
//                        cover = imageService.fetchCoverInformation(it.cover)?.toCoverFromUpdate(),
//                        title = it.title!!,
//                        pubDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(it.creationDate!!), ZoneId.of("Europe/Paris")),
//                        description = it.description!!
//                ) }
//                .toSet()
//    }
//
//    override fun blockingSignatureOf(url: URI): String {
//        return USER_NAME_EXTRACTOR.on(url.toASCIIString()).group(1)
//                .toOption()
//                .map { API_LIST_OF_ITEMS.format(it) }
//                .map { signatureService.fromUrl(it) }
//                .getOrElse { throw RuntimeException("Username not Found") }
//    }
//
//    override fun type() = Type("Dailymotion", "Dailymotion")
//
//    override fun compatibility(url: String?) =
//            if ("www.dailymotion.com" in (url ?: "")) 1
//            else Integer.MAX_VALUE
//
//    companion object {
//        const val API_LIST_OF_ITEMS = "https://api.dailymotion.com/user/%s/videos?fields=created_time,description,id,thumbnail_720_url,title"
//        // http://www.dailymotion.com/karimdebbache
//        private val USER_NAME_EXTRACTOR = from("^.+dailymotion.com/(.*)")
//        private const val ITEM_URL = "http://www.dailymotion.com/video/%s"
//        private val LIST_DAILYMOTION_VIDEO_DETAIL_TYPE = object : TypeRef<Set<DailymotionUpdaterVideoDetail>>(){}
//    }
//}
//
//private class DailymotionUpdaterVideoDetail(
//        val id: String? = null,
//        val title: String? = null,
//        val description: String? = null,
//        @JsonProperty("created_time") val creationDate: Long? = null,
//        @JsonProperty("thumbnail_720_url") val cover: String? = null
//)
