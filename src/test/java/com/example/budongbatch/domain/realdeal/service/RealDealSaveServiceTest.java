package com.example.budongbatch.domain.realdeal.service;

import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.fixture.RealDealFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RealDealSaveServiceTest {

    @Autowired
    private RealDealSaveService realDealSaveService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("delete from real_deals");
        ensureUniqueConstraint();
    }

    @Test
    @DisplayName("INSERT IGNORE로 중복 데이터는 스킵하고 신규만 저장한다")
    void saveAllIgnoreDuplicates_skipsDuplicates() {
        RealDeal d1 = RealDealFixture.create();
        RealDeal d2 = RealDealFixture.create(); // 동일 키

        int saved = realDealSaveService.saveAllIgnoreDuplicates(List.of(d1, d2));

        Integer count = jdbcTemplate.queryForObject("select count(*) from real_deals", Integer.class);
        assertThat(saved).isEqualTo(1);
        assertThat(count).isEqualTo(1);
    }

    private void ensureUniqueConstraint() {
        Integer cnt = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where table_name='REAL_DEALS' and constraint_name='UK_REAL_DEAL_DEDUP'",
                Integer.class
        );
        if (cnt != null && cnt == 0) {
            jdbcTemplate.execute(
                    "alter table real_deals add constraint uk_real_deal_dedup " +
                            "unique (property_name, address, deal_amount, deal_date, floor)"
            );
        }
    }
}
