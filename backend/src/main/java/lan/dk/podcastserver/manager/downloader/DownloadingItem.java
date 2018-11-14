package lan.dk.podcastserver.manager.downloader;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;

/**
 * Created by kevin on 03/12/2017
 */
public class DownloadingItem {
    private final Item item;
    private final List<String> urls;
    private final String filename;
    private final String userAgent;

    @java.beans.ConstructorProperties({"item", "urls", "filename", "userAgent"})
    public DownloadingItem(Item item, List<String> urls, String filename, String userAgent) {
        this.item = item;
        this.urls = urls;
        this.filename = filename;
        this.userAgent = userAgent;
    }

    public static DownloadingItemBuilder builder() {
        return new DownloadingItemBuilder();
    }

    public Option<String> url() {
        return urls.headOption();
    }

    public Item getItem() {
        return this.item;
    }

    public List<String> getUrls() {
        return this.urls;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DownloadingItem)) return false;
        final DownloadingItem other = (DownloadingItem) o;
        final Object this$item = this.getItem();
        final Object other$item = other.getItem();
        if (this$item == null ? other$item != null : !this$item.equals(other$item)) return false;
        final Object this$urls = this.getUrls();
        final Object other$urls = other.getUrls();
        if (this$urls == null ? other$urls != null : !this$urls.equals(other$urls)) return false;
        final Object this$filename = this.getFilename();
        final Object other$filename = other.getFilename();
        if (this$filename == null ? other$filename != null : !this$filename.equals(other$filename)) return false;
        final Object this$userAgent = this.getUserAgent();
        final Object other$userAgent = other.getUserAgent();
        if (this$userAgent == null ? other$userAgent != null : !this$userAgent.equals(other$userAgent)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $item = this.getItem();
        result = result * PRIME + ($item == null ? 43 : $item.hashCode());
        final Object $urls = this.getUrls();
        result = result * PRIME + ($urls == null ? 43 : $urls.hashCode());
        final Object $filename = this.getFilename();
        result = result * PRIME + ($filename == null ? 43 : $filename.hashCode());
        final Object $userAgent = this.getUserAgent();
        result = result * PRIME + ($userAgent == null ? 43 : $userAgent.hashCode());
        return result;
    }

    public String toString() {
        return "DownloadingItem(item=" + this.getItem() + ", urls=" + this.getUrls() + ", filename=" + this.getFilename() + ", userAgent=" + this.getUserAgent() + ")";
    }

    public static class DownloadingItemBuilder {
        private Item item;
        private List<String> urls;
        private String filename;
        private String userAgent;

        DownloadingItemBuilder() {
        }

        public DownloadingItem.DownloadingItemBuilder item(Item item) {
            this.item = item;
            return this;
        }

        public DownloadingItem.DownloadingItemBuilder urls(List<String> urls) {
            this.urls = urls;
            return this;
        }

        public DownloadingItem.DownloadingItemBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public DownloadingItem.DownloadingItemBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public DownloadingItem build() {
            return new DownloadingItem(item, urls, filename, userAgent);
        }

        public String toString() {
            return "DownloadingItem.DownloadingItemBuilder(item=" + this.item + ", urls=" + this.urls + ", filename=" + this.filename + ", userAgent=" + this.userAgent + ")";
        }
    }
}
