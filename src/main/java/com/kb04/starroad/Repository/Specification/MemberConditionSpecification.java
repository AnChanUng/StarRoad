package com.kb04.starroad.Repository.Specification;

import com.kb04.starroad.Entity.MemberCondition;
import org.springframework.data.jpa.domain.Specification;

public class MemberConditionSpecification {

    public static Specification<MemberCondition> equalsMemberNo(int memberNo) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("member").get("no"), memberNo);
    }
}
