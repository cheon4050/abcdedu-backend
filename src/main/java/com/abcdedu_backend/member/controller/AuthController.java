package com.abcdedu_backend.member.controller;

import com.abcdedu_backend.exception.ApplicationException;
import com.abcdedu_backend.exception.ErrorCode;
import com.abcdedu_backend.exception.ErrorResponse;
import com.abcdedu_backend.member.dto.LoginTokenDTO;
import com.abcdedu_backend.member.dto.request.LoginRequest;
import com.abcdedu_backend.member.dto.request.SignUpRequest;
import com.abcdedu_backend.member.dto.response.LoginResponse;
import com.abcdedu_backend.member.dto.response.ReissueResponse;
import com.abcdedu_backend.member.service.MemberService;
import com.abcdedu_backend.utils.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 요청이 완료되었습니다.", content = @Content),
        @ApiResponse(responseCode = "400", description = "잘못된 요청입니다. (RequestBody Validation)", content = @Content),
        @ApiResponse(responseCode = "500", description = "서버 에러", content = @Content)
})
@Tag(name = "인증 기능", description = "로그인/회원가입 등 인증 관련 api입니다.")
public class AuthController {

    private final MemberService memberService;

    @Operation(summary = "회원 가입", description = "회원가입을 합니다.")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일입니다.", content = @Content),
    })
    @PostMapping("/signup")
    public Response<Void> signUp(@Valid @RequestBody SignUpRequest signUpRequest){
        memberService.signUp(signUpRequest);
        return Response.success();
    }

    @Operation(summary = "로그인", description = "로그인을 합니다.")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "400", description = "존재하지 않는 이메일 또는 패스워드입니다.", content = @Content),
    })
    @PostMapping("/login")
    public Response<LoginResponse> login(HttpServletResponse response, @Valid @RequestBody LoginRequest loginRequest){
        LoginTokenDTO loginTokenDto = memberService.login(loginRequest);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginTokenDto.refreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(14).toSeconds())
                .build();
        response.setHeader("Set-Cookie", refreshTokenCookie.toString());
        LoginResponse loginResponse = new LoginResponse(loginTokenDto.accessToken());
        return Response.success(loginResponse);
    }

    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급 합니다.")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰입니다.",content = @Content),
            @ApiResponse(responseCode = "401", description = "토큰이 존재하지 않습니다.",content = @Content),
    })
    @GetMapping("/reissue")
    public Response<ReissueResponse> reissue(HttpServletRequest request) {
        Cookie refreshTokenCookie = Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.TOKEN_NOT_FOUND));
        String refreshToken = refreshTokenCookie.getValue();
        ReissueResponse reissueResponse = memberService.reissue(refreshToken);
        return Response.success(reissueResponse);
    }

}
