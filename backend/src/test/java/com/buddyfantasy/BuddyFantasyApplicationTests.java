package com.buddyfantasy;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestConfig.class)
class BuddyFantasyApplicationTests {

    @Autowired
    DataSource dataSource;

    @Test
    void contextLoadsAndFlywayAppliesBaseline() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer tables = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_name in ('event_publication', 'shedlock')""", Integer.class);
        assertThat(tables).isEqualTo(2);
    }
}
