package org.jenkinsci.plugins.schedulebuild;

import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class JCasCGlobalConfigurationTest extends RoundTripAbstractTest {

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent) {
    }

    @Override
    protected String stringInLogExpected() {
        return "timeZone = Europe/Paris";
    }

    @Override
    protected String configResource() {
        return "scheduleBuild-default-time-zone.yaml";
    }
}
