package com.autovision.platform.intake;

import jakarta.validation.constraints.NotBlank;

public record ControlledDatasetIntakeRequest(
        @NotBlank String packageReference
) {
}