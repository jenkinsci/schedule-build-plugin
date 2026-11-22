package org.jenkinsci.plugins.schedulebuild;

import hudson.model.Cause;
import java.io.Serial;
import java.io.Serializable;

public class ScheduledBuildCause extends Cause.UserIdCause implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
