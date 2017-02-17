package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;

import java.util.Collection;
import java.util.Collections;

@Extension
public final class ScheduleBuildTransientProjectActionFactory extends TransientActionFactory<Job> {
   
    @Override
    public Class<Job> type()
    {
        return Job.class;
    }

    @Override
    public Collection<? extends Action> createFor(Job target)
    {
        return Collections.singleton(new ScheduleBuildAction(target));
    }
}
