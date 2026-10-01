package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.Date;

@Getter
@Entity
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SequenceGenerator(name = "policy_seq", sequenceName = "policy_seq", allocationSize = 1)
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policy_seq")
    private int no;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false, length = 2000)
    private String explain;

    @Column(nullable = false, length = 50)
    private String tag;

    @Column(nullable = false, length = 5000)
    private String link;

    @Column(nullable = false)
    private Date endDate;

    /**
     * 청년정책 한 건.
     *
     * @param location 지역 (서울, 경기, 중앙부처)
     * @param tag      분류 (금융지원, 교육, 생활지원, 금융자산 형성)
     * @param endDate  신청 마감일
     */
    public static Policy of(String name, String location, String tag, String explain,
                            String link, Date endDate) {
        return Policy.builder()
                .name(name)
                .location(location)
                .tag(tag)
                .explain(explain)
                .link(link)
                .endDate(endDate)
                .build();
    }
}
