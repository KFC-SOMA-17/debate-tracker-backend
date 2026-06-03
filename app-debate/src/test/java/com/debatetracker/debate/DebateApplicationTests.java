package com.debatetracker.debate;

import com.debatetracker.debate.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DebateApplicationTests {

	@Test
	void contextLoads() {
	}
}
