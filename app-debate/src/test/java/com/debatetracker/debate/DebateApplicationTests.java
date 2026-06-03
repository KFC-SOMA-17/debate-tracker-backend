package com.debatetracker.debate;

import com.debatetracker.debate.config.TestcontainersConfiguration;
import com.debatetracker.infra.stt.client.SttClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DebateApplicationTests {

	@MockitoBean
	private SttClient sttClient;

	@Test
	void contextLoads() {
	}
}
