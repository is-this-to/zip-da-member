package com.zipdamember.domain.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증/인가 약관 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class AuthController {

}