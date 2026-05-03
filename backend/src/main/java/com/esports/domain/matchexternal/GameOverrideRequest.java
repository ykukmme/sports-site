package com.esports.domain.matchexternal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// 게임 보정 요청 DTO.
// 모든 필드 nullable: null = 변경하지 않음(no-op), 값 = override 적용, 빈 리스트 = clear.
public class GameOverrideRequest {

    @Min(0)
    private Integer durationSec;

    @Min(0)
    private Integer blueTeamGold;

    @Min(0)
    private Integer redTeamGold;

    // 골드 분배. null = 미변경, 빈 리스트 = override 제거(clear), 10행 = 새 override 적용.
    @Valid
    private List<DistributionEntryRequest> goldDistribution;

    @Valid
    private List<DistributionEntryRequest> damageDistribution;

    @Size(max = 500)
    private String note;

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public Integer getBlueTeamGold() {
        return blueTeamGold;
    }

    public void setBlueTeamGold(Integer blueTeamGold) {
        this.blueTeamGold = blueTeamGold;
    }

    public Integer getRedTeamGold() {
        return redTeamGold;
    }

    public void setRedTeamGold(Integer redTeamGold) {
        this.redTeamGold = redTeamGold;
    }

    public List<DistributionEntryRequest> getGoldDistribution() {
        return goldDistribution;
    }

    public void setGoldDistribution(List<DistributionEntryRequest> goldDistribution) {
        this.goldDistribution = goldDistribution;
    }

    public List<DistributionEntryRequest> getDamageDistribution() {
        return damageDistribution;
    }

    public void setDamageDistribution(List<DistributionEntryRequest> damageDistribution) {
        this.damageDistribution = damageDistribution;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    // 분배 단일 행. side ∈ {BLUE,RED}, position ∈ {TOP,JUNGLE,MID,ADC,SUPPORT}.
    public static class DistributionEntryRequest {

        @NotBlank
        private String side;

        @NotBlank
        private String position;

        @DecimalMin("0")
        @DecimalMax("100")
        private BigDecimal percent;

        @DecimalMin("0")
        private BigDecimal perMinute;

        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }

        public String getPosition() {
            return position;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public BigDecimal getPercent() {
            return percent;
        }

        public void setPercent(BigDecimal percent) {
            this.percent = percent;
        }

        public BigDecimal getPerMinute() {
            return perMinute;
        }

        public void setPerMinute(BigDecimal perMinute) {
            this.perMinute = perMinute;
        }
    }
}
