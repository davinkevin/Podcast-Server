package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Podcast;


public interface Updater {

   public Podcast updateFeed(Podcast podcast);
   public Podcast findPodcast(String url);
   public String signaturePodcast(Podcast podcast);
}
