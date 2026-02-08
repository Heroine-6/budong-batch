package com.example.budongbatch.domain.realdeal.repository;

import com.example.budongbatch.domain.realdeal.entity.BatchDealCollectFailedLawd;
import com.example.budongbatch.domain.realdeal.entity.BatchDealCollectFailedLawdId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchDealCollectFailedLawdRepository extends JpaRepository<BatchDealCollectFailedLawd, BatchDealCollectFailedLawdId> {
    List<BatchDealCollectFailedLawd> findByDealYmd(String dealYmd);
    void deleteByDealYmd(String dealYmd);
}
