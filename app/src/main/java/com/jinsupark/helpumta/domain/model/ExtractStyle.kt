package com.jinsupark.helpumta.domain.model

enum class ExtractTemplate { STRIP, GRID, CARD, TYPO }

enum class ExtractRatio { STORY, FEED, SQUARE }

/**
 * 사용자가 고르는 "논리적인" 색. 실제 색값은 템플릿이 정한다.
 * (카드는 흰 배경 → 베이지, 검정 배경 → 차콜로 바꿔 그린다)
 */
enum class ExtractColor { ORANGE, BLACK, WHITE }

data class ExtractPalette(
    val background: ExtractColor,
    val text: ExtractColor
) {
    val isValid: Boolean get() = background != text

    companion object {
        /** 배경 3 × 글씨 3에서 같은 색끼리 겹치는 조합을 뺀 6개 */
        val ALL: List<ExtractPalette> = listOf(
            ExtractPalette(ExtractColor.BLACK, ExtractColor.WHITE),
            ExtractPalette(ExtractColor.WHITE, ExtractColor.BLACK),
            ExtractPalette(ExtractColor.ORANGE, ExtractColor.WHITE),
            ExtractPalette(ExtractColor.ORANGE, ExtractColor.BLACK),
            ExtractPalette(ExtractColor.WHITE, ExtractColor.ORANGE),
            ExtractPalette(ExtractColor.BLACK, ExtractColor.ORANGE)
        )
    }
}

/** 템플릿의 대표 이미지 색 */
fun ExtractTemplate.defaultPalette(): ExtractPalette = when (this) {
    ExtractTemplate.STRIP, ExtractTemplate.GRID -> ExtractPalette(ExtractColor.BLACK, ExtractColor.WHITE)
    ExtractTemplate.CARD -> ExtractPalette(ExtractColor.WHITE, ExtractColor.BLACK)
    ExtractTemplate.TYPO -> ExtractPalette(ExtractColor.ORANGE, ExtractColor.WHITE)
}

data class ExtractStyle(
    val template: ExtractTemplate = ExtractTemplate.STRIP,
    val ratio: ExtractRatio = ExtractRatio.STORY,
    val showHeader: Boolean = true,
    val showComment: Boolean = true,
    val includeEmpty: Boolean = true,
    val showLogo: Boolean = true,
    /**
     * 사용자가 직접 고른 색만 템플릿별로 들어 있다.
     * 하나로 공유하면 타이포에서 주황을 고르고 스트립으로 넘어갔을 때 스트립도 주황이 된다.
     */
    val palettes: Map<ExtractTemplate, ExtractPalette> = emptyMap()
) {
    /** 현재 템플릿에 적용할 색 — 고른 적 없으면 대표 색 */
    val palette: ExtractPalette
        get() = palettes[template]?.takeIf { it.isValid } ?: template.defaultPalette()

    /**
     * 로고를 실제로 그릴지.
     * 스트립·그리드·카드는 로고가 헤더 안에 있어서 헤더를 끄면 같이 사라진다.
     * 저장된 showLogo는 건드리지 않는다 — 헤더를 다시 켜면 로고도 그대로 돌아온다.
     */
    val isLogoVisible: Boolean
        get() = showLogo && (template == ExtractTemplate.TYPO || showHeader)

    /** 로고 온오프를 누를 수 있는 상태인지 (헤더가 꺼진 동안은 비활성) */
    val isLogoToggleEnabled: Boolean
        get() = template == ExtractTemplate.TYPO || showHeader

    fun withPalette(palette: ExtractPalette): ExtractStyle =
        copy(palettes = palettes + (template to palette))
}