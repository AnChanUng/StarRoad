package com.kb04.starroad.Repository.Specification;

import com.kb04.starroad.Entity.Policy;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.*;


public class PolicySpecification {
    public static Specification<Policy> searchPolicyWithMultiConditions(Map<String, ?> conditions){
        return(((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            for(String key: conditions.keySet()) {
                switch (key) {
                    case "location":
                        predicates.add(criteriaBuilder.equal(root.get(key), String.valueOf(conditions.get(key))));
                        break;
                    case "keyword":
                        // escape 문자를 명시한다. 생략하면 H2(Oracle 모드)에서 escape '' 가 NULL 로 처리돼 검색 결과가 비게 된다
                        predicates.add(criteriaBuilder.like(root.get("name"), '%' + String.valueOf(conditions.get(key)) + '%', '\\'));
                        break;
                    case "tag":

                        predicates.add(criteriaBuilder.and(root.get("tag").in((Collection<?>) conditions.get(key))));
                        break;
                }
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }));
    }


}
