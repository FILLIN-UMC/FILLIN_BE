package com.fillin.domain.enums;

import com.fillin.domain.AlarmContext;

//알림의 키고 끄기에서 각 항목은 다음처럼 묶습니다
/*
    내 제보에 대한 피드백 알림 - REPORT
    피드백 알림 - LIKE, LEVEL_UP, EXPIRATION
    서비스 알림 - NOTICE

    따라서 제보 알림을 끌 경우 타입이 REPORT인 알림은 전부 안오도록, 피드백을 끌 경우 관련 타입 알림은 전부 안오도록 처리
*/
public enum AlarmType {

    REPORT {
        @Override
        public String buildMessage(AlarmContext ctx) {
            return ctx.getActorName() + "님이 회원님의 제보에\n‘"
                    + ctx.getFeedback() + "’ 피드백을 남겼어요";
        }
    },

    LIKE {
        @Override
        public String buildMessage(AlarmContext ctx) {
            return ctx.getActorName() + "님이 회원님의 제보에\n.좋아요를 눌렀어요";
        }
    },

    LEVEL_UP {
        @Override
        public String buildMessage(AlarmContext ctx) {
            return ctx.getBadgeName() + " 뱃지를 획득했어요!\n"
                    + "총 " + ctx.getCount() + "개의 제보를 완료했어요";
        }
    },

    EXPIRATION {
        @Override
        public String buildMessage(AlarmContext ctx) {
            return "내 제보가 " + ctx.getCount() + "일 뒤 사라져요";
        }
    },

    NOTICE {
        @Override
        public String buildMessage(AlarmContext ctx) {
            return ctx.getMessage();
        }
    };

    public abstract String buildMessage(AlarmContext ctx);
}