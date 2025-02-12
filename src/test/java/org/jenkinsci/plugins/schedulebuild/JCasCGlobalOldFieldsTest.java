package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import jenkins.model.GlobalConfiguration;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JCasCGlobalOldFieldsTest extends AbstractRoundTripTest {

    @Override
    protected void assertConfiguredAsExpected(JenkinsRule j, String configContent) {
        ScheduleBuildGlobalConfiguration globalConfig =
                GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);
        assertThat(globalConfig, is(not(nullValue())));
        assertThat(globalConfig.getDefaultStartTime(), is("00:34:56"));
        assertThat(globalConfig.getTimeZone(), is("Europe/Rome"));
    }

    @Override
    protected String stringInLogExpected() {
        return "timeZone = Europe/Rome";
    }

    @Override
    protected String configResource() {
        return "scheduleBuild-old-fields.yaml";
    }
}
