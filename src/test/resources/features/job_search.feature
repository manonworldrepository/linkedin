Feature: Job Search with LinkedIn API

  Scenario: Search for jobs by query and country
    Given I search for jobs with query "\"Java\"" in country "Germany"
    When the request is sent to the API
    Then I should get a list of direct application URLs