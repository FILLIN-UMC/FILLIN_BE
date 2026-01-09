package com.fillin.domain.enums;

//알림의 키고 끄기에서 각 항목은 다음처럼 묶습니다
/*
    제보 알림 - REPORT
    피드백 알림 - LIKE, LEVEL_UP, EXPIRATION
    서비스 알림 - NOTICE

    따라서 제보 알림을 끌 경우 타입이 REPORT인 알림은 전부 안오도록, 피드백을 끌 경우 관련 타입 알림은 전부 안오도록 처리
*/
public enum AlarmType { NOTICE, LIKE, LEVEL_UP, EXPIRATION, REPORT }
