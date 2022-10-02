package com.eastshine.batch.task.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
