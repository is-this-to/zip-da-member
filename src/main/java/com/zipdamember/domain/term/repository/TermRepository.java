package com.zipdamember.domain.term.repository;

import com.zipdamember.domain.term.entity.Term;
import com.zipdamember.domain.term.response.TermResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<List<TermResponse>> findAllBy();
}
