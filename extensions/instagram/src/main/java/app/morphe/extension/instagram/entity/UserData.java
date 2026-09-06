/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */


package app.morphe.extension.instagram.entity;

import app.morphe.extension.instagram.utils.PikoLog;
import com.instagram.common.typedurl.ImageUrl;

public class UserData extends Entity {
    private final Object obj;

    public UserData(Object obj) {
        super(obj);
        this.obj = obj;
    }

    private Object getAdditionalUserInfo() {
        if (this.obj == null) return null;
        try {
            Object res = super.getField(this.obj, "fieldName");
            if (res != null && !(res instanceof Number)) {
                return res;
            }
        } catch (Exception ignored) {
        }
        return this.obj;
    }

    private Object invokeUserMethod(Object target, String methodName) throws Exception {
        if (target == null) return null;
        try {
            return super.getMethod(target, methodName);
        } catch (NoSuchMethodException e) {
            if (target != this.obj && this.obj != null) {
                return super.getMethod(this.obj, methodName);
            }
            throw e;
        }
    }

    public Boolean isVerified() throws Exception {
        Object target = getAdditionalUserInfo();
        if (target != null) {
            try {
                Object res = invokeUserMethod(target, "isVerified");
                if (res instanceof Boolean) return (Boolean) res;
            } catch (Exception e) {
                PikoLog.e("UserData", "Error at isVerified", e);
            }
        }
        if (this.obj != null) {
            try {
                Object res = super.getMethod(this.obj, "A5s");
                if (res instanceof Boolean) return (Boolean) res;
            } catch (Exception ignored) {
            }
            try {
                Object res = super.getMethod(this.obj, "isVerified");
                if (res instanceof Boolean) return (Boolean) res;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public String getUsername() throws Exception {
        Object target = getAdditionalUserInfo();
        if (target != null) {
            try {
                Object uname = invokeUserMethod(target, "methodName");
                if (uname != null && !uname.toString().isEmpty()) return uname.toString();
            } catch (Exception ignored) {
            }
        }
        if (this.obj != null) {
            try {
                Object uname = super.getMethod(this.obj, "A86");
                if (uname != null && !uname.toString().isEmpty()) return uname.toString();
            } catch (Exception ignored) {
            }
            try {
                Object uname = super.getMethod(this.obj, "getUsername");
                if (uname != null && !uname.toString().isEmpty()) return uname.toString();
            } catch (Exception ignored) {
            }
        }
        return "instagram_user";
    }

    public String getFullName() throws Exception {
        Object target = getAdditionalUserInfo();
        if (target != null) {
            try {
                Object name = invokeUserMethod(target, "methodName");
                if (name != null && !name.toString().isEmpty()) return name.toString();
            } catch (Exception ignored) {
            }
        }
        if (this.obj != null) {
            try {
                Object name = super.getMethod(this.obj, "A7N");
                if (name != null && !name.toString().isEmpty()) return name.toString();
            } catch (Exception ignored) {
            }
        }
        return this.getUsername();
    }

    public String getBio() throws Exception {
        Object target = getAdditionalUserInfo();
        if (target != null) {
            try {
                Object bio = invokeUserMethod(target, "BCu");
                if (bio != null) return bio.toString();
            } catch (Exception e) {
                PikoLog.e("UserData", "Error at getBio", e);
            }
        }
        if (this.obj != null) {
            try {
                Object bio = super.getMethod(this.obj, "A6y");
                if (bio != null) return bio.toString();
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    public String getProfilePictureUrl() throws Exception {
        Object target = getAdditionalUserInfo();
        if (target != null) {
            try {
                Object profilePicObject = invokeUserMethod(target, "Bvt");
                if (profilePicObject != null) {
                    Entity profilePicEntity = new Entity(profilePicObject);
                    Object url = profilePicEntity.getMethod("getUrl");
                    if (url != null && !url.toString().isEmpty()) {
                        return url.toString();
                    }
                }
            } catch (Exception e) {
                PikoLog.e("UserData", "Error at getProfilePictureUrl", e);
            }
        }
        if (this.obj != null) {
            try {
                Object picObj = super.getMethod(this.obj, "A1B");
                if (picObj != null) {
                    Object url = new Entity(picObj).getMethod("getUrl");
                    if (url != null && !url.toString().isEmpty()) return url.toString();
                }
            } catch (Exception ignored) {
            }
            try {
                Object lowRes = super.getMethod(this.obj, "A1d");
                if (lowRes != null) {
                    Object url = new Entity(lowRes).getMethod("getUrl");
                    if (url != null && !url.toString().isEmpty()) return url.toString();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public ImageUrl getLowResProfilePicture() throws Exception {
        Object target = getAdditionalUserInfo();
        if (target != null) {
            try {
                Object imageUrlObject = invokeUserMethod(target, "mediaName");
                if (imageUrlObject instanceof ImageUrl) {
                    return (ImageUrl) imageUrlObject;
                }
            } catch (Exception e) {
                PikoLog.e("UserData", "Error at getLowResProfilePicture", e);
            }
        }
        if (this.obj != null) {
            try {
                Object lowRes = super.getMethod(this.obj, "A1d");
                if (lowRes instanceof ImageUrl) return (ImageUrl) lowRes;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** The public permalink of this profile: what "share" and "copy link" both hand out. */
    public String getProfileLink() throws Exception {
        return "https://www.instagram.com/" + getUsername() + "/";
    }

    public String getUserId() throws Exception {
        if (this.obj == null) return "";
        try {
            Object id = super.getMethod(this.obj, "getId");
            return id != null ? id.toString() : "";
        } catch (Exception e) {
            PikoLog.e("UserData", "Error at getUserId", e);
            return "";
        }
    }

    public UserFriendshipStatus getUserFriendshipStatus() throws Exception {
        Object target = getAdditionalUserInfo();
        if (target != null) {
            try {
                Object friendshipStatusObject = invokeUserMethod(target, "methodname");
                if (friendshipStatusObject != null) {
                    return new UserFriendshipStatus(friendshipStatusObject);
                }
            } catch (Exception e) {
                PikoLog.e("UserData", "Error at getUserFriendshipStatus", e);
            }
        }
        if (this.obj != null) {
            try {
                Object statusObj = super.getMethod(this.obj, "A1i");
                if (statusObj != null) {
                    return new UserFriendshipStatus(statusObj);
                }
            } catch (Exception ignored) {
            }
        }
        return new UserFriendshipStatus(null);
    }
}