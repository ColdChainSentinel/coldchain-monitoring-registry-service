package pe.edu.upc.coldchain.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/ep02/hu04.feature")
@ConfigurationParameter(key = "cucumber.glue", value = "pe.edu.upc.coldchain.acceptance")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty,html:target/cucumber/hu04.html,json:target/cucumber/hu04.json,junit:target/cucumber/hu04.xml")
@ConfigurationParameter(key = "cucumber.execution.parallel.enabled", value = "false")
@ConfigurationParameter(key = "cucumber.publish.enabled", value = "false")
public class Hu04AcceptanceTest { }
