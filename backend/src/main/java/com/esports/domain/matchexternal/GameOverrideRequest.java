package com.esports.domain.matchexternal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// 게임 보정 요청 DTO — 정수 필드(durationSec, blueTeamGold, redTeamGold) + note.
// 분배는 별도 PUT /distribution/{kind}/{side} 엔드포인트로 분리됨.
// 모든 필드 nullable: null = 변경하지 않음(no-op).
public class GameOverrideRequest {

    @Min(0)
    private Integer durationSec;

    @Min(0)
    private Integer blueTeamGold;

    @Min(0)
    private Integer redTeamGold;

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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
