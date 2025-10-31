package com.nhis.fraud.service;

import com.nhis.fraud.dto.ChartDataDto;
import com.nhis.fraud.entity.Claim;
import com.nhis.fraud.entity.ScoreCategory;
import com.nhis.fraud.repository.ClaimRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartService {

    private final ClaimRepository claimRepository;

    public ChartService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public List<ChartDataDto> getTimeSeriesData(LocalDate startDate, LocalDate endDate) {
        List<Claim> claims = claimRepository.findAll();

        if (startDate != null) {
            claims = claims.stream()
                    .filter(c -> c.getEncounterDate() != null && !c.getEncounterDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }
        if (endDate != null) {
            claims = claims.stream()
                    .filter(c -> c.getEncounterDate() != null && !c.getEncounterDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        Map<LocalDate, List<Claim>> byDate = claims.stream()
                .filter(c -> c.getEncounterDate() != null)
                .collect(Collectors.groupingBy(Claim::getEncounterDate));

        List<ChartDataDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Claim>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Claim> dayClaims = entry.getValue();

            long count = dayClaims.size();
            double avgScore = dayClaims.stream()
                    .filter(c -> c.getFraudScore() != null)
                    .mapToInt(Claim::getFraudScore)
                    .average()
                    .orElse(0.0);

            BigDecimal totalAmount = dayClaims.stream()
                    .filter(c -> c.getAmountBilled() != null)
                    .map(Claim::getAmountBilled)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long low = dayClaims.stream().filter(c -> c.getScoreCategory() == ScoreCategory.LOW).count();
            long med = dayClaims.stream().filter(c -> c.getScoreCategory() == ScoreCategory.MEDIUM).count();
            long high = dayClaims.stream().filter(c -> c.getScoreCategory() == ScoreCategory.HIGH).count();

            result.add(new ChartDataDto(date, count, avgScore, totalAmount, low, med, high));
        }

        result.sort(Comparator.comparing(ChartDataDto::date));
        return result;
    }
}

