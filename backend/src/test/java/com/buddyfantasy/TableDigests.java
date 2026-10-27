package com.buddyfantasy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * A digest of the rows of every table of the schema, read from {@code information_schema} so new tables are covered:
 * equal digests before and after something mean it wrote nothing.
 */
@UtilityClass
public class TableDigests {

    /** Tables that change on any request or run, whatever it does. */
    public final List<String> ALWAYS_CHANGING = List.of("flyway_schema_history", "spring_session",
            "spring_session_attributes", "shedlock");

    /** The digest of each table but {@code excluded}, by table name. */
    public Map<String, String> of(JdbcOperations jdbc, Collection<String> excluded) {
        Map<String, String> digests = new LinkedHashMap<>();
        for (String table : jdbc.queryForList("""
                select quote_ident(table_name) from information_schema.tables
                where table_schema = current_schema() and table_type = 'BASE TABLE' order by table_name""",
                String.class)) {
            if (!excluded.contains(table)) {
                digests.put(table, jdbc.queryForObject(
                        "select coalesce(md5(string_agg(t::text, ',' order by t::text)), '') from " + table + " t",
                        String.class));
            }
        }
        return digests;
    }
}
