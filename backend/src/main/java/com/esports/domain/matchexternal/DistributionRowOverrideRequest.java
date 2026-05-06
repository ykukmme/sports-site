package com.esports.domain.matchexternal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// 분배 행 단위 보정 요청 — (kind, side) 묶음 단위.
// rows 는 정확히 5행 (TOP/JUNGLE/MID/ADC/SUPPORT 한 진영).
public class DistributionRowOverrideRequest {

    @Valid
    @NotEmpty
    @Size(min = 5, max = 5)
    private List<RowEntry> rows;

    @Size(max = 500)
    private String note;

    public List<RowEntry> getRows() {
        return rows;
    }

    public void setRows(List<RowEntry> rows) {
        this.rows = rows;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    // 단일 라이너 행. position 은 enum 5종 중 하나.
    public static class RowEntry {

        @Pattern(regexp = "TOP|JUNGLE|MID|ADC|SUPPORT")
        @NotNull
        private String position;

        @NotNull
        @DecimalMin("0")
        @DecimalMax("100")
        private BigDecimal percent;

        @NotNull
        @DecimalMin("0")
        private BigDecimal perMinute;

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
