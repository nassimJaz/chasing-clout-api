package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Finish;
import com.takima.backskeleton.models.PulledCard;

public record PulledCardDto(int position, CardDto card, Finish finish) {
    public static PulledCardDto from(PulledCard pulled) {
        return new PulledCardDto(pulled.getPosition(), CardDto.from(pulled.getMemeCard()), pulled.getFinish());
    }
}
