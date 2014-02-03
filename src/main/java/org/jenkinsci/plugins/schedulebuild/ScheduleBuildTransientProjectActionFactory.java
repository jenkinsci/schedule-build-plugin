package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;

import java.util.Collection;
import java.util.Collections;

@Extension
public final class ScheduleBuildTransientProjectActionFactory extends TransientProjectActionFactory {
    @Override
    public Collection<? extends Action> createFor(final AbstractProject target) {
        return Collections.singleton(new ScheduleBuildAction(target));
    }
}
