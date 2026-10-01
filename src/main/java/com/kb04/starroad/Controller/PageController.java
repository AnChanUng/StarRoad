package com.kb04.starroad.Controller;

import com.kb04.starroad.Config.LoginMemberArgumentResolver;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 화면(JSP)만 내려 주는 컨트롤러.
 *
 * <p>모델에 데이터를 담지 않는다. 화면은 뜬 뒤에 {@code /api/starroad/**} 를 호출해 데이터를 받아 그린다.
 * 로그인이 필요한 화면은 로그인 화면으로 돌려보낸다.
 */
@Hidden
@Controller
public class PageController {

    private static final String LOGIN_PAGE = "redirect:/starroad/login";

    @GetMapping("/starroad")
    public String home(HttpSession session) {
        return isLoggedIn(session) ? "loginHome" : "home";
    }

    @GetMapping("/starroad/login")
    public String login() {
        return "member/login";
    }

    @GetMapping("/starroad/member")
    public String member() {
        return "member/member";
    }

    @GetMapping("/starroad/board/main")
    public String boardMain() {
        return "board/main";
    }

    @GetMapping({"/starroad/board/free", "/starroad/board/popular"})
    public String boardList() {
        return "board/board";
    }

    @GetMapping("/starroad/board/detail")
    public String boardDetail() {
        return "board/detail";
    }

    @GetMapping("/starroad/board/write")
    public String boardWrite(HttpSession session, RedirectAttributes redirectAttributes) {
        return loginRequired(session, redirectAttributes, "board/write", "게시물 쓰기는 로그인이 필요한 서비스입니다");
    }

    @GetMapping("/starroad/board/update")
    public String boardUpdate(HttpSession session, RedirectAttributes redirectAttributes) {
        return loginRequired(session, redirectAttributes, "board/update", "게시글 수정은 로그인이 필요한 서비스입니다");
    }

    @GetMapping("/starroad/comment/update")
    public String commentUpdate(HttpSession session, RedirectAttributes redirectAttributes) {
        return loginRequired(session, redirectAttributes, "comment/update", "댓글 수정은 로그인이 필요한 서비스입니다");
    }

    @GetMapping({"/starroad/mypage", "/starroad/mypage/asset"})
    public String mypageAsset(HttpSession session, RedirectAttributes redirectAttributes) {
        return mypage(session, redirectAttributes, "mypage/asset");
    }

    @GetMapping("/starroad/mypage/board")
    public String mypageBoard(HttpSession session, RedirectAttributes redirectAttributes) {
        return mypage(session, redirectAttributes, "mypage/board");
    }

    @GetMapping("/starroad/mypage/comment")
    public String mypageComment(HttpSession session, RedirectAttributes redirectAttributes) {
        return mypage(session, redirectAttributes, "mypage/comment");
    }

    @GetMapping("/starroad/mypage/challenge")
    public String mypageChallenge(HttpSession session, RedirectAttributes redirectAttributes) {
        return mypage(session, redirectAttributes, "mypage/challenge");
    }

    @GetMapping("/starroad/mypage/reward")
    public String mypageReward(HttpSession session, RedirectAttributes redirectAttributes) {
        return mypage(session, redirectAttributes, "mypage/reward");
    }

    @GetMapping("/starroad/mypage/info")
    public String mypageInfo(HttpSession session, RedirectAttributes redirectAttributes) {
        return mypage(session, redirectAttributes, "mypage/info");
    }

    @GetMapping("/starroad/mypage/password")
    public String mypagePassword(HttpSession session, RedirectAttributes redirectAttributes) {
        return mypage(session, redirectAttributes, "mypage/password");
    }

    // 전체 조회와 검색 결과는 같은 화면이다. 화면이 주소창의 검색 조건을 읽어 API 를 호출한다
    @GetMapping({"/starroad/policy", "/starroad/policy/result"})
    public String policy() {
        return "policy/policy";
    }

    @GetMapping({"/starroad/product", "/starroad/product/result"})
    public String product() {
        return "product/product";
    }

    @GetMapping("/starroad/chat")
    public String chat() {
        return "chat/chat";
    }

    private String mypage(HttpSession session, RedirectAttributes redirectAttributes, String view) {
        return loginRequired(session, redirectAttributes, view, "마이페이지는 로그인이 필요한 서비스입니다");
    }

    /** 로그인했으면 view 를, 아니면 안내 문구와 함께 로그인 화면으로 보낸다 */
    private String loginRequired(HttpSession session, RedirectAttributes redirectAttributes,
                                 String view, String message) {
        if (isLoggedIn(session)) {
            return view;
        }
        redirectAttributes.addFlashAttribute("error", message);
        return LOGIN_PAGE;
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(LoginMemberArgumentResolver.SESSION_KEY) != null;
    }
}
