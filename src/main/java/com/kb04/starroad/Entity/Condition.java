package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;


@Getter
@Entity
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "condition")
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "condition_seq")
    @SequenceGenerator(name = "condition_seq", sequenceName = "CONDITION_SEQ", allocationSize = 1)
    @Column(name = "no")
    private int no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_no", nullable = false)
    private Product prod;

    @Column(name = "condition_name", nullable = false, length = 100)
    private String conditionName;

    @Column(name = "rate", nullable = false)
    private Double rate;

    /** 상품의 우대 조건 하나. 충족하면 rate(%p) 만큼 금리가 더해진다. */
    public static Condition of(Product prod, String conditionName, Double rate) {
        return Condition.builder()
                .prod(prod)
                .conditionName(conditionName)
                .rate(rate)
                .build();
    }

}
