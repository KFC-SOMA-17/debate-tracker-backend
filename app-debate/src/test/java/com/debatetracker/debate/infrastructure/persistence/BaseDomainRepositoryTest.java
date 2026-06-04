package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.DatabaseCleaner;
import com.debatetracker.debate.config.TestcontainersConfiguration;
import com.debatetracker.debate.fixture.TestFixtureConfiguration;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateJpaRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import({TestcontainersConfiguration.class, TestFixtureConfiguration.class})
@ExtendWith(DatabaseCleaner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class BaseDomainRepositoryTest {

    @Autowired
    protected DebateJpaRepository debateJpaRepository;
}
