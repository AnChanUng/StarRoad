package com.kb04.starroad.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 세션의 로그인 회원({@code MemberDto})을 넣어 준다.
 *
 * <p>{@code required = true}(기본)인데 로그인하지 않았다면 401 로 응답한다.
 * 비로그인도 허용하는 조회 API 는 {@code required = false} 로 두고 null 을 받는다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {

    boolean required() default true;
}
