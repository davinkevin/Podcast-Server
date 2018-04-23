package lan.dk.podcastserver.manager.worker.mycanal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static io.vavr.API.Option;

/**
 * Created by kevin on 26/12/2017
 */
class MyCanalModel {

    private static final String MYCANAL_DATE_PATTERN = "dd/MM/yyyy-HH:mm:ss";

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalPageItem {
        @Getter @Setter String displayName;
        @Getter @Setter String pathname;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalItem {
        @Getter @Setter String contentID;
        @Getter @Setter String image;
        @Getter @Setter MyCanalItemOnClick onClick;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalItemOnClick {
        @Getter @Setter String displayName;
        @Getter @Setter String path;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalDetailsItem {
        @JsonProperty("ID") @Getter @Setter String id;
        @JsonProperty("DURATION") @Getter @Setter Long duration;
        @JsonProperty("INFOS") @Getter @Setter MyCanalInfosItem infos;
        @JsonProperty("MEDIA") @Getter @Setter MyCanalMediaItem media;
        @JsonProperty("URL") @Getter @Setter String url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalInfosItem {
        @JsonProperty("DESCRIPTION") @Getter @Setter String description;
        @JsonProperty("PUBLICATION") @Getter @Setter MyCanalPublicationItem publication;
        @JsonProperty("TITRAGE") @Getter @Setter MyCanalTitrageItem titrage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalPublicationItem {
        @JsonProperty("DATE") @Getter @Setter String date;
        @JsonProperty("HEURE") @Getter @Setter String heure;

        ZonedDateTime asZonedDateTime() {
            LocalDateTime localDateTime = LocalDateTime.parse(date.concat("-").concat(heure), DateTimeFormatter.ofPattern(MYCANAL_DATE_PATTERN));
            return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalTitrageItem {
        @JsonProperty("TITRE") @Getter @Setter String titre;
        @JsonProperty("SOUS_TITRE") @Getter @Setter String sous_titre;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalMediaItem {
        @JsonProperty("IMAGES") @Getter @Setter MyCanalImageItem images;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalImageItem {
        @JsonProperty("GRAND") @Getter @Setter String grand;
        @JsonProperty("PETIT") @Getter @Setter String petit;

        Option<String> cover() {
            return Option(grand).orElse(Option(petit));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MyCanalVideoItem {
        @JsonProperty("BAS_DEBIT") @Getter @Setter String bas_debit;
        @JsonProperty("HAUT_DEBIT") @Getter @Setter String haut_debit;
        @JsonProperty("HD") @Getter @Setter String hd;
        @JsonProperty("MOBILE") @Getter @Setter String mobile;
        @JsonProperty("HDS") @Getter @Setter String hds;
        @JsonProperty("HLS") @Getter @Setter String hls;
    }
    
}
