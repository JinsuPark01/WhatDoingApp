package com.jinsupark.helpumta.domain.model

enum class ExtractTemplate { STRIP, GRID }

enum class ExtractRatio { STORY, FEED, SQUARE }   // 9:16 / 4:5 / 1:1

/**
 * 사진 추출 스타일. 마지막으로 쓴 설정을 기기에 저장해 다음 추출 때 그대로 연다.
 * 색은 옵션으로 두지 않는다 — 템플릿이 색을 정해야 글자 대비가 보장된다.
 */
data class ExtractStyle(
    val template: ExtractTemplate = ExtractTemplate.STRIP,
    val ratio: ExtractRatio = ExtractRatio.STORY,
    val showHeader: Boolean = true,     // 그룹명·날짜
    val showComment: Boolean = true,    // 한줄 소감
    val includeEmpty: Boolean = true    // 기록 없는 멤버
)