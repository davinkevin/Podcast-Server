package lan.dk.podcastserver.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Cover {

    public static final Cover DEFAULT_COVER = new Cover();

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;
    private String url;
    private Integer width;
    private Integer height;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cover)) return false;

        Cover cover = (Cover) o;
        return new EqualsBuilder()
                .append(StringUtils.lowerCase(url), StringUtils.lowerCase(cover.url))
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(StringUtils.lowerCase(url))
                .toHashCode();
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public UUID getId() {
        return this.id;
    }

    public String getUrl() {
        return this.url;
    }

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }
}
