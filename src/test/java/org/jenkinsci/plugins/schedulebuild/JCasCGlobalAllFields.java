package org.jenkinsci.plugins.schedulebuild;

import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import java.util.TimeZone;
import jenkins.model.GlobalConfiguration;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JCasCGlobalAllFields extends RoundTripAbstractTest {

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent) {
        ScheduleBuildGlobalConfiguration globalConfig = GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);
        assertThat(globalConfig, is(not(nullValue())));
        assertThat(globalConfig.getDefaultScheduleTime(), is("12:34:56 AM"));
        assertThat(globalConfig.getTimeZone(), is("Europe/Rome"));
    }

    @Override
    protected String stringInLogExpected() {
        return "timeZone = Europe/Rome";
    }

    @Override
    protected String configResource() {
        return "scheduleBuild-all-fields.yaml";
    }
}
