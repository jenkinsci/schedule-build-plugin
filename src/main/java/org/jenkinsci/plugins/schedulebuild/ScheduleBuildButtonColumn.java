package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class ScheduleBuildButtonColumn extends ListViewColumn {
    public static final class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ScheduleBuildButtonColumn_DisplayName();
        }

        @Override
        public ListViewColumn newInstance(final StaplerRequest request, final JSONObject formData)
                throws FormException {
            return new ScheduleBuildButtonColumn();
        }

        @Override
        public boolean shownByDefault() {
            return true;
        }
    }

    @Extension public static final Descriptor<ListViewColumn> DESCRIPTOR = new DescriptorImpl();

    @Override
    public Descriptor<ListViewColumn> getDescriptor() {
        return DESCRIPTOR;
    }
}
