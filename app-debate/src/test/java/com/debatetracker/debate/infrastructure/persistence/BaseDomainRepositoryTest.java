package com.debatetracker.debate.infrastructure.persistence;

import com.debatetracker.debate.DatabaseCleaner;
import com.debatetracker.debate.infrastructure.persistence.jpa.debate.DebateJpaRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@ExtendWith(DatabaseCleaner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class BaseDomainRepositoryTest {

    @Autowired
    protected DebateJpaRepository debateJpaRepository;
}
