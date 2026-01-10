package ee.jobs.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Eesti IT tööpakkumiste agregaatori peaklass.
 * Käivitab Spring Boot rakenduse ja võimaldab ajastatud ülesandeid.
 */
@SpringBootApplication
@EnableScheduling
public class JobsAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobsAggregatorApplication.class, args);
    }
}
