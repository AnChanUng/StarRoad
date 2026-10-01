package com.kb04.starroad.Repository;

import com.kb04.starroad.Entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByIdAndPassword(String id, String password);

    Member findByNo(int no);

    /** 회원이 적금에 납입한 금액 합계. 납입 내역이 없으면 null */
    @Query("SELECT SUM(s.price) FROM PaymentLog p JOIN p.subscription s JOIN s.prod pr "
            + "WHERE s.member.no = :no AND pr.type = 'S'")
    Long getSavings(@Param("no") int no);

    /** 회원이 예금에 납입한 금액 합계. 납입 내역이 없으면 null */
    @Query("SELECT SUM(s.price) FROM PaymentLog p JOIN p.subscription s JOIN s.prod pr "
            + "WHERE s.member.no = :no AND pr.type = 'D'")
    Long getDeposit(@Param("no") int no);

    Optional<Member> findById(String id);

    // Member 엔티티의 필드를 이용하여 데이터베이스 업데이트를 수행하는 쿼리 메소드
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.phone = :phone, m.email = :email, m.address = :address, " +
            "m.job = :job, m.salary = :salary, m.purpose = :purpose, m.source = :source, " +
            "m.goal = :goal WHERE m.no = :no")
    void updateMember(@Param("no") int no,
                      @Param("phone") String phone,
                      @Param("email") String email,
                      @Param("address") String address,
                      @Param("job") String job,
                      @Param("salary") int salary,
                      @Param("purpose") String purpose,
                      @Param("source") String source,
                      @Param("goal") int goal);

    Member findByEmail(String email);

    @Transactional
    @Modifying
    @Query("UPDATE Member m SET m.password = :encPass WHERE m.id = :id")
    void findByIdAndUpdatePassword(@Param("id") String id, @Param("encPass") String encPass);
}