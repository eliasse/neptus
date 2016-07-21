package pt.lsts.neptus.plugins.mvplanning.planning.constraints;

import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;

/**
 * Created by tsmarques on 21/07/16.
 */
public class HasPayload extends TaskConstraint {
    private Profile profile;

    public HasPayload(Profile profile) {
        this.profile = profile;
    }

    @Override
    public boolean isValidated(Object value) {
        return profile.getProfileVehicles().contains(value);
    }
}
