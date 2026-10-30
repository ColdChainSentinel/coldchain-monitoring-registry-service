package pe.edu.upc.coldchain.integration;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/integration")
@ConfigurationParameter(key = "cucumber.glue", value = "pe.edu.upc.coldchain.integration")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty,html:target/cucumber/flow.html,json:target/cucumber/flow.json,junit:target/cucumber/flow.xml")
@ConfigurationParameter(key = "cucumber.execution.parallel.enabled", value = "false")
@ConfigurationParameter(key = "cucumber.publish.enabled", value = "false")
public class ColdChainAcceptanceIT { }
