package com.fillin.repository.mission;

import com.fillin.domain.Member;
import com.fillin.domain.MemberMission;
import com.fillin.domain.enums.ReportCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberMissionRepository extends JpaRepository<MemberMission, Long> {
    List<MemberMission> findByMemberAndMission_CategoryAndIsCompleteFalse(Member member, ReportCategory category);
}
