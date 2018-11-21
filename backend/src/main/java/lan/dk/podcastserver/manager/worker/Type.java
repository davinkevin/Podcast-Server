package lan.dk.podcastserver.manager.worker;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kevin on 25/12/2017
 */
public class Type {
    private final String key;
    private final String name;

    public Type(String key, String name) {
        this.key = key;
        this.name = name;
    }

    @JsonProperty("key")
    public String key() {
        return key;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Type)) return false;
        final Type other = (Type) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$key = this.key;
        final Object other$key = other.key;
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        final Object this$name = this.name;
        final Object other$name = other.name;
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $key = this.key;
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        final Object $name = this.name;
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Type;
    }
}
