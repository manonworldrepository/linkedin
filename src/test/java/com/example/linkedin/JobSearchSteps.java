package com.example.linkedin;

import com.example.linkedin.model.Job;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@CucumberContextConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LinkedInMockServer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JobSearchSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<List<Job>> response;

    @Given("I search for jobs with query {string} in country {string}")
    public void searchJobs(String query, String country) {
        response = restTemplate.exchange(
                "/jobs/search?query=" + query + "&country=" + country,
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Job>>() {} // Expecting List<Job>
        );
    }

    @When("the request is sent to the API")
    public void sendRequest() {
        assertNotNull(response);
    }

    @Then("I should get a list of direct application URLs")
    public void validateJobUrls() {
        List<Job> jobs = response.getBody();
        assertNotNull(jobs, "Response body should not be null");
        assertFalse(jobs.isEmpty(), "Jobs list should not be empty");

        List<String> jobUrls = jobs.stream()
                .map(Job::applyUrl)
                .toList();

        assertNotNull(jobUrls, "Extracted job URLs list should not be null"); // Keep this for null-check of jobUrls itself

        assertTrue(jobUrls.stream().allMatch(url -> url != null && url.startsWith("http")),
                "All job URLs should start with 'http' and not be null");
    }

    @Then("none of the jobs should have Easy Apply enabled")
    public void validateNoEasyApply() {
    }
}