package steps;

import io.cucumber.java.en.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathSteps {

    private int a, b, result;

    @Given("I have two numbers {int} and {int}")
    public void i_have_two_numbers(Integer x, Integer y) {
        a = x;
        b = y;
    }

    @When("I add them")
    public void i_add_them() {
        result = a + b;
    }

    @Then("the result should be {int}")
    public void the_result_should_be(Integer expected) {
        assertEquals(expected, result);
    }
}
