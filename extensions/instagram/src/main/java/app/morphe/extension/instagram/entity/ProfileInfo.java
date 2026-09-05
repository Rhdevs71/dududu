/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */


package app.morphe.extension.instagram.entity;


public class ProfileInfo extends Entity {
    protected final Object obj;

    public ProfileInfo(Object obj) {
        super(obj);
        this.obj = obj;
    }

    private Entity getProfileRelatedDetails() throws Exception {
        return super.getFieldAsEntity("fieldName");
    }

    private Entity getUserDetailViewModel() throws Exception {
        return super.getFieldAsEntity("fieldName");
    }

    public boolean isSelfProfile() throws Exception {
        Entity profileRelatedDetails = getProfileRelatedDetails();
        if (profileRelatedDetails == null) return false;
        Object val = profileRelatedDetails.getField("fieldName");
        return Boolean.TRUE.equals(val);
    }

    public UserData getUserData() throws Exception {
        Entity userDetailViewModel = getUserDetailViewModel();
        if (userDetailViewModel == null) return new UserData(null);
        Object userDataObject = userDetailViewModel.getField("fieldName");
        return new UserData(userDataObject);
    }

}
