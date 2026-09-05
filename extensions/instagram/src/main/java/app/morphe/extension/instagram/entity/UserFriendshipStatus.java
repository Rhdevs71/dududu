/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
*/

package app.morphe.extension.instagram.entity;

import java.util.Map;
import java.util.HashMap;
import com.instagram.user.model.FriendshipStatus;
import app.morphe.extension.crimera.PikoUtils;

public class UserFriendshipStatus extends Entity {
    private final Object obj;

    public UserFriendshipStatus(Object obj) {
        super(obj);
        this.obj = obj;
    }

    public Map<String, Boolean> getMappings(){
        if (this.obj == null) {
            return new HashMap<>();
        }
        try {
            Class<?> helperClass = Class.forName("classname");
            Map result = (Map) super.getMethod(helperClass, "methodname", new Class[]{FriendshipStatus.class}, this.obj);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            PikoUtils.logger(e);
        }
        return new HashMap<>();
    }

    private Boolean getValue(String key) {
        Map<String, Boolean> mappings = getMappings();
        return Boolean.TRUE.equals(mappings.get(key));
    }

    public Boolean getFollowBackStatus() {
        return getValue("followed_by");
    }

    public Boolean getFollowingStatus() {
        return getValue("following");
    }
}