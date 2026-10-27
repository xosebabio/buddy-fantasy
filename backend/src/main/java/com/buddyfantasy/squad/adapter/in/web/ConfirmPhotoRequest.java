package com.buddyfantasy.squad.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

record ConfirmPhotoRequest(@NotBlank String photoKey) {
}
