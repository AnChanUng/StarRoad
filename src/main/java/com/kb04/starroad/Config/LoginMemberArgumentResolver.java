package com.kb04.starroad.Config;

import com.kb04.starroad.Dto.MemberDto;
import com.kb04.starroad.Exception.ErrorCode;
import com.kb04.starroad.Exception.StarroadException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** {@link LoginMember} 가 붙은 {@code MemberDto} 파라미터를 세션에서 꺼내 채운다. */
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    /** 로그인 회원을 세션에 담는 이름. JSP 도 같은 이름으로 읽는다. */
    public static final String SESSION_KEY = "currentUser";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && MemberDto.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpSession session = webRequest.getNativeRequest(HttpServletRequest.class).getSession(false);
        Object loginMember = session == null ? null : session.getAttribute(SESSION_KEY);

        if (loginMember == null && parameter.getParameterAnnotation(LoginMember.class).required()) {
            throw new StarroadException(ErrorCode.LOGIN_REQUIRED);
        }
        return loginMember;
    }
}
