package com.kb04.starroad.Entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SequenceGenerator(name = "payment_log_seq", sequenceName="payment_log_seq")
@Table(name = "payment_log")
public class PaymentLog {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="payment_log_seq")
    private int no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_no", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private Date paymentDate;

    /** 가입한 상품에 paymentDate 에 납입한 기록 */
    public static PaymentLog of(Subscription subscription, Date paymentDate) {
        return PaymentLog.builder()
                .subscription(subscription)
                .paymentDate(paymentDate)
                .build();
    }
}
