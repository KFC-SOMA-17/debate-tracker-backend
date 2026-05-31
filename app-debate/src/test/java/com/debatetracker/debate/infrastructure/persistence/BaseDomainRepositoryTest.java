package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.DatabaseCleaner;
import com.debatetracker.debate.config.TestcontainersConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@ExtendWith(DatabaseCleaner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class BaseDomainRepositoryTest {

}
