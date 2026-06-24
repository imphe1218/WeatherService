1.) This project is built using the following:
  - Apache Maven 3.9.14
  - Java 17.0.12
    
2.) You need the following API Keys and set their corresponding environment variables which is wired to the project. This follows
    the idea of not storing secrets in plaintext within the project. In our case we are simulating that these secrests are stored
    somewhere separate from the project and can be referenced in our configuration files from the environment:
  - WeatherStack API Key, read https://weatherstack.com/documentation how to request an API Key.
    - create an environment variable named: WEATHERSTACK_API_KEY
    - This is the API Key in order to call the WeatherStack provider API downstream.
  - OPENWEATHERMAP_API_KEY, read https://openweathermap.org/current how to request an API key.
    - create an environment variable named: OPENWEATHERMAP_API_KEY
    - This is the API Key in order to call OpenWeatherMap provider API downstream.
  - NVD_API_KEY, visit https://nvd.nist.gov/developers/request-an-api-key to request an API key.
    - create an environment variable named: NVD_API_KEY
    - This is the API Key in order to update our local vulnerability list database for OWASP Dependecy Check.
    - This is OPTIONAL only if you want to update your local vulnerability list database and you can do this by uncommenting
      the following in the POM:
        <!--<goals>
          <goal>check</goal>
        </goals>-->
    - By default this is not needed as the local vul list db update from from NIST is taking so long with the update API
      call faling at times. You can try on your end and see if the update successfully finishes.

3.) The following libraries are added to enforce quality gates instead of relying on human to review:
  - PMD for static analysis of the source for potential bugs and bad practices.
  - Checkstyle for enforcing coding standards and formatting rules.
  - SpotBugs for analyzing real potential runtime bugs.
  - FindSecBugs for analyzing security vulnerabilities in the code.
  - OWASP Dependency Check for scanning the project dependencies for security vulnerabilities.

4.) The project is using the following frameworks:
  - Spring Boot 3.2.12 for ease of REST API development.
  - Jacoco for unit test execution during maven build and unit tests reporting including coverage report.
  - SLF4J/Logback for logging.
  - Resilien4J for implementing Circuit Breaker.
  - JUnit Jupiter and Mockito for unit testing and mocking.
