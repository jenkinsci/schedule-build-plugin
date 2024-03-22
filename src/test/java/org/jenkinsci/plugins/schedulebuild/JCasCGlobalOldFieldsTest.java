package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.GlobalConfiguration;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class JCasCGlobalOldFieldsTest extends RoundTripAbstractTest {

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent) {
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
