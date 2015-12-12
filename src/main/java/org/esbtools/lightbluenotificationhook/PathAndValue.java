package org.esbtools.lightbluenotificationhook;

import java.util.Objects;

public class PathAndValue {
    private String path;
    private String value;

    public PathAndValue() {
    }

    public PathAndValue(String path, String value) {
        this.path = path;
        this.value = value;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathAndValue identityValue = (PathAndValue) o;
        return Objects.equals(path, identityValue.path) &&
                Objects.equals(value, identityValue.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, value);
    }

    @Override
    public String toString() {
        return "Identity{" +
                "path='" + path + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
