package lan.dk.podcastserver.manager.worker.mycanal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.control.Option;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static io.vavr.API.Option;

/**
 * Created by kevin on 26/12/2017
 */
public class MyCanalModel {

    private static final String MYCANAL_DATE_PATTERN = "dd/MM/yyyy-HH:mm:ss";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalPageItem {
        String displayName;
        String pathname;

        public String getDisplayName() {
            return this.displayName;
        }

        public String getPathname() {
            return this.pathname;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public void setPathname(String pathname) {
            this.pathname = pathname;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalItem {
        String contentID;
        String image;
        MyCanalItemOnClick onClick;

        public String getContentID() {
            return this.contentID;
        }

        public String getImage() {
            return this.image;
        }

        public MyCanalItemOnClick getOnClick() {
            return this.onClick;
        }

        public void setContentID(String contentID) {
            this.contentID = contentID;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public void setOnClick(MyCanalItemOnClick onClick) {
            this.onClick = onClick;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalItemOnClick {
        String displayName;
        String path;

        public String getDisplayName() {
            return this.displayName;
        }

        public String getPath() {
            return this.path;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalDetailsItem {
        @JsonProperty("ID")
        String id;
        @JsonProperty("DURATION")
        Long duration;
        @JsonProperty("INFOS")
        MyCanalInfosItem infos;
        @JsonProperty("MEDIA")
        MyCanalMediaItem media;
        @JsonProperty("URL")
        String url;

        public String getId() {
            return this.id;
        }

        public Long getDuration() {
            return this.duration;
        }

        public MyCanalInfosItem getInfos() {
            return this.infos;
        }

        public MyCanalMediaItem getMedia() {
            return this.media;
        }

        public String getUrl() {
            return this.url;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }

        public void setInfos(MyCanalInfosItem infos) {
            this.infos = infos;
        }

        public void setMedia(MyCanalMediaItem media) {
            this.media = media;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalInfosItem {
        @JsonProperty("DESCRIPTION")
        String description;
        @JsonProperty("PUBLICATION")
        MyCanalPublicationItem publication;
        @JsonProperty("TITRAGE")
        MyCanalTitrageItem titrage;

        public String getDescription() {
            return this.description;
        }

        public MyCanalPublicationItem getPublication() {
            return this.publication;
        }

        public MyCanalTitrageItem getTitrage() {
            return this.titrage;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setPublication(MyCanalPublicationItem publication) {
            this.publication = publication;
        }

        public void setTitrage(MyCanalTitrageItem titrage) {
            this.titrage = titrage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalPublicationItem {
        @JsonProperty("DATE")
        String date;
        @JsonProperty("HEURE")
        String heure;

        public ZonedDateTime asZonedDateTime() {
            LocalDateTime localDateTime = LocalDateTime.parse(date.concat("-").concat(heure), DateTimeFormatter.ofPattern(MYCANAL_DATE_PATTERN));
            return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
        }

        public String getDate() {
            return this.date;
        }

        public String getHeure() {
            return this.heure;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public void setHeure(String heure) {
            this.heure = heure;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalTitrageItem {
        @JsonProperty("TITRE")
        String titre;
        @JsonProperty("SOUS_TITRE")
        String sous_titre;

        public String getTitre() {
            return this.titre;
        }

        public String getSous_titre() {
            return this.sous_titre;
        }

        public void setTitre(String titre) {
            this.titre = titre;
        }

        public void setSous_titre(String sous_titre) {
            this.sous_titre = sous_titre;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalMediaItem {
        @JsonProperty("IMAGES")
        MyCanalImageItem images;

        public MyCanalImageItem getImages() {
            return this.images;
        }

        public void setImages(MyCanalImageItem images) {
            this.images = images;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalImageItem {
        @JsonProperty("GRAND")
        String grand;
        @JsonProperty("PETIT")
        String petit;

        public Option<String> cover() {
            return Option(grand).orElse(Option(petit));
        }

        public String getGrand() {
            return this.grand;
        }

        public String getPetit() {
            return this.petit;
        }

        public void setGrand(String grand) {
            this.grand = grand;
        }

        public void setPetit(String petit) {
            this.petit = petit;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MyCanalVideoItem {
        @JsonProperty("BAS_DEBIT")
        String bas_debit;
        @JsonProperty("HAUT_DEBIT")
        String haut_debit;
        @JsonProperty("HD")
        String hd;
        @JsonProperty("MOBILE")
        String mobile;
        @JsonProperty("HDS")
        String hds;
        @JsonProperty("HLS")
        String hls;

        public String getBas_debit() {
            return this.bas_debit;
        }

        public String getHaut_debit() {
            return this.haut_debit;
        }

        public String getHd() {
            return this.hd;
        }

        public String getMobile() {
            return this.mobile;
        }

        public String getHds() {
            return this.hds;
        }

        public String getHls() {
            return this.hls;
        }

        public void setBas_debit(String bas_debit) {
            this.bas_debit = bas_debit;
        }

        public void setHaut_debit(String haut_debit) {
            this.haut_debit = haut_debit;
        }

        public void setHd(String hd) {
            this.hd = hd;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public void setHds(String hds) {
            this.hds = hds;
        }

        public void setHls(String hls) {
            this.hls = hls;
        }
    }
    
}
