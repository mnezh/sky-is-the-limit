package steps;

import context.ScenarioContext;
import io.cucumber.java.Scenario;

public abstract class BaseSteps {
  public Scenario scenario;
  public ScenarioContext ctx;

  public void log(String message) {
    scenario.log(message);
  }
}
