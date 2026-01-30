package com.fillin.domain.enums;

//총 제보 갯수에 따라 0~9개 루키, 10~29 베테랑, 30~ 마스터
public enum Achievement {
    ROOKIE, VETERAN, MASTER;

    public static Achievement resolveAchievement(int count) {
        if(count <= 9){
            return Achievement.ROOKIE;
        }
        else if(count <= 29){
            return Achievement.VETERAN;
        }
        else return Achievement.MASTER;
    }
}
