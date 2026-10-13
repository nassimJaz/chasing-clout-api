package com.takima.backskeleton.DTO;

// The JWT to send back as "Authorization: Bearer <token>".
public record TokenDto(String token) {
}
