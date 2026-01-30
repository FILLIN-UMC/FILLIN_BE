package com.fillin.repository;

import com.fillin.domain.Member;
import com.fillin.domain.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotiSetRepository extends JpaRepository<NotificationSetting,Long> {
    NotificationSetting findByMember(Member member);
}
