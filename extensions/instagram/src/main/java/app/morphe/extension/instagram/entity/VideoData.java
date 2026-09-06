/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */


package app.morphe.extension.instagram.entity;

import java.util.Map;
import java.util.HashMap;
import app.morphe.extension.instagram.utils.PikoLog;
import app.morphe.extension.crimera.downloader.MediaType;

public class VideoData extends Entity implements MediaInterface {
    private final boolean isPandoVideoVersion;

    public VideoData(Object obj) {
        super(obj);
        this.isPandoVideoVersion = obj != null && obj.getClass().getName().contains("ImmutablePando");
    }

    private Map immutablePandoVideoVersionMap() {
        try {
            Object res = super.getMethod("methodname");
            if (res instanceof Map) {
                return (Map) res;
            }
        } catch (Exception e) {
            PikoLog.e("VideoData", "Error at immutablePandoVideoVersionMap", e);
        }
        return new HashMap();
    }

    private Map videoVersionMap() {
        try {
            Object res = super.getMethod("methodname");
            if (res instanceof Map) {
                return (Map) res;
            }
        } catch (Exception e) {
            PikoLog.e("VideoData", "Error at videoVersionMap", e);
        }
        return new HashMap();
    }

    public Integer getHeight() throws Exception {
        if (this.isPandoVideoVersion) {
            return (Integer) this.immutablePandoVideoVersionMap().getOrDefault("height", 0);
        }
        return (Integer) this.videoVersionMap().getOrDefault("height", 0);
    }

    public Integer getWidth() throws Exception {
        if (this.isPandoVideoVersion) {
            return (Integer) this.immutablePandoVideoVersionMap().getOrDefault("width", 0);
        }
        return (Integer) this.videoVersionMap().getOrDefault("width", 0);
    }

    private Integer getCodec() throws Exception {
        if (this.isPandoVideoVersion) {
            return (Integer) this.immutablePandoVideoVersionMap().getOrDefault("type", 0);
        }
        return (Integer) this.videoVersionMap().getOrDefault("type", 0);
    }

    public String getVariantTag() {
        try {
            return this.getHeight() + "x" + this.getWidth() + "-" + this.getCodec();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String getUrl() throws Exception {
        if (this.obj == null) return null;
        Object url = super.getMethod(this.obj, "getUrl");
        return url != null ? url.toString() : null;
    }

    public MediaType getMediaType() {
        return MediaType.VIDEO;
    }

}