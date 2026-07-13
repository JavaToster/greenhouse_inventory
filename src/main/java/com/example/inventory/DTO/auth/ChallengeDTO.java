package com.example.inventory.DTO.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Challenge issued for device authentication")
public record ChallengeDTO(

        @Schema(
                description = "Random challenge string that must be signed by the device",
                example = "a8f5f167f44f4964e6c998dee827110c"
        )
        String challenge

) {
}