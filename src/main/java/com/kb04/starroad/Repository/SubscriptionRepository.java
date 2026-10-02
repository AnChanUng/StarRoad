package com.kb04.starroad.Repository;

import com.kb04.starroad.Entity.Member;
import com.kb04.starroad.Entity.Product;
import com.kb04.starroad.Entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer>, JpaSpecificationExecutor<Subscription> {
    public List<Subscription> findByMember(Member no);
    public List<Subscription> findByMemberNo(int memberNo);
    public Subscription findByNo(int no);
}
