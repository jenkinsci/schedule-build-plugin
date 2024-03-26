package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.TransientActionFactory;

@Extension
public final class ScheduleBuildTransientProjectActionFactory extends TransientActionFactory<Job> {

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Override
    public Collection<? extends Action> createFor(Job target) {
        if (target instanceof TopLevelItem) {
            return Collections.singleton(new ScheduleBuildAction(target));
        } else {
            return Collections.emptyList();
        }
    }
}
