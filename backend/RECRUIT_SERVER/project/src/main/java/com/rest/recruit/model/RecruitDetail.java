package com.rest.recruit.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecruitDetail {
    private int recruitId;
    //추가
    private int companyId;
    private String companyName;
    private String imageFileName;
    private String employmentPageUrl;
    private String startTime;
    private String endTime;
    private int recruitType;
    private String content;
    private int viewCount;
    private int favoriteCount;
    //추가
    private int favorite;
}