package com.kb04.starroad.Service;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Dto.auth.LoginRequestDto;
import com.kb04.starroad.Entity.Member;
import com.kb04.starroad.Exception.ErrorCode;
import com.kb04.starroad.Exception.StarroadException;
import com.kb04.starroad.Repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    @Autowired
    private MemberRepository memberRepository;

    /**
     * 로그인 기능
     * @return 세션에 담을 회원 정보. 아이디가 없거나 비밀번호가 틀리면 401
     */
    public MemberDto authenticate(LoginRequestDto loginRequestDto) {
        String id = loginRequestDto.getId();
        String password = loginRequestDto.getPassword();

        if (!StringUtils.hasText(id)) {
            throw new StarroadException(ErrorCode.LOGIN_ID_REQUIRED);
        }
        if (!StringUtils.hasText(password)) {
            throw new StarroadException(ErrorCode.LOGIN_PASSWORD_REQUIRED);
        }

        Member member = memberRepository.findById(id)
                .orElseThrow(AuthService::loginFailed);

        // 입력한 비밀번호와 데이터베이스에서 가져온 암호화된 비밀번호를 비교
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, member.getPassword())) {
            throw loginFailed();
        }
        return MemberDto.from(member);
    }

    // 아이디가 없는지 비밀번호가 틀렸는지는 알려 주지 않는다
    private static StarroadException loginFailed() {
        return new StarroadException(ErrorCode.LOGIN_FAILED);
    }
}
