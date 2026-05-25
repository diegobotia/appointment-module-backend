package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class SpecialistJpaRepositoryImpl implements SpecialistJpaRepositoryCustom {

    private final EntityManager em;

    public SpecialistJpaRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<Specialist> search(String q, String numDoc, String registro, String specialty, Boolean active, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Main query
        CriteriaQuery<Specialist> cq = cb.createQuery(Specialist.class);
        Root<Specialist> root = cq.from(Specialist.class);
        List<Predicate> predicates = new ArrayList<>();

        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
        }
        if (numDoc != null) {
            predicates.add(cb.equal(root.get("numDoc"), numDoc));
        }
        if (registro != null) {
            predicates.add(cb.equal(root.get("numeroMedico"), registro));
        }
        if (specialty != null) {
            predicates.add(cb.equal(cb.lower(root.get("specialty")), specialty.toLowerCase()));
        }

        if (q != null) {
            String pattern = "%" + q + "%";
            Predicate fullName = cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), pattern.toLowerCase());
            Predicate first = cb.like(cb.lower(root.get("firstName")), pattern.toLowerCase());
            Predicate last = cb.like(cb.lower(root.get("lastName")), pattern.toLowerCase());
            predicates.add(cb.or(fullName, first, last));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(root.get("lastName")), cb.asc(root.get("firstName")));

        TypedQuery<Specialist> query = em.createQuery(cq);
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        query.setFirstResult(pageNumber * pageSize);
        query.setMaxResults(pageSize);
        List<Specialist> content = query.getResultList();

        // Count query
        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<Specialist> countRoot = countQ.from(Specialist.class);
        List<Predicate> countPreds = new ArrayList<>();

        if (active != null) {
            countPreds.add(cb.equal(countRoot.get("active"), active));
        }
        if (numDoc != null) {
            countPreds.add(cb.equal(countRoot.get("numDoc"), numDoc));
        }
        if (registro != null) {
            countPreds.add(cb.equal(countRoot.get("numeroMedico"), registro));
        }
        if (specialty != null) {
            countPreds.add(cb.equal(cb.lower(countRoot.get("specialty")), specialty.toLowerCase()));
        }
        if (q != null) {
            String pattern = "%" + q + "%";
            Predicate fullName = cb.like(cb.lower(cb.concat(cb.concat(countRoot.get("firstName"), " "), countRoot.get("lastName"))), pattern.toLowerCase());
            Predicate first = cb.like(cb.lower(countRoot.get("firstName")), pattern.toLowerCase());
            Predicate last = cb.like(cb.lower(countRoot.get("lastName")), pattern.toLowerCase());
            countPreds.add(cb.or(fullName, first, last));
        }

        countQ.select(cb.count(countRoot)).where(countPreds.toArray(new Predicate[0]));
        Long total = em.createQuery(countQ).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
