package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Finish;

// One (card, finish) bought from the system.
public record CardRequest(Long memeCardId, Finish finish) {
}
