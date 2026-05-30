package com.debatetracker.debate.service;

import com.debatetracker.debate.DatabaseCleaner;
import com.debatetracker.debate.domain.debate.DebateRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@ExtendWith(DatabaseCleaner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class BaseServiceTest {

    @Autowired
    protected DebateRepository debateRepository;

}
