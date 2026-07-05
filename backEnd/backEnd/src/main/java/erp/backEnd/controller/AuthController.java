package erp.backEnd.controller;

import erp.backEnd.dto.member.LoginRequest;
import erp.backEnd.dto.member.LoginResponse;
import erp.backEnd.entity.Member;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증(Auth)", description = "세션 기반 로그인·로그아웃 및 현재 로그인 사용자 조회 API")
@RestController("authController")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Operation(summary = "로그인", description = "아이디/비밀번호로 인증하고 세션(JSESSIONID)을 발급한다. 이후 요청은 세션 쿠키로 인증이 유지된다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getLoginId(), req.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // SecurityContext 를 세션에 저장 → 이후 요청은 JSESSIONID 로 인증 유지
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        Member member = memberRepository.findByLoginId(req.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(LoginResponse.from(member));
    }

    @Operation(summary = "현재 로그인 사용자 조회", description = "세션의 인증 정보로 현재 로그인한 사용자를 반환한다. 미인증 시 401.")
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Member member = memberRepository.findByLoginId(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(LoginResponse.from(member));
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 무효화하고 SecurityContext 를 비운다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }
}
