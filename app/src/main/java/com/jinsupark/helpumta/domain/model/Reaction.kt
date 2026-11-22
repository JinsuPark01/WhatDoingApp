package com.jinsupark.helpumta.domain.model

/**
 * 기록에 남기는 응원 반응. 한 사람이 한 기록에 하나만.
 * 저장값은 이모지가 아니라 key — 이모지(표현)를 바꿔도 데이터가 그대로 유지된다.
 * key 목록은 firestore.rules의 허용 목록과 같아야 한다.
 */
enum class Reaction(val key: String) {
    MUSCLE("muscle"),
    FIRE("fire"),
    CLAP("clap");

    companion object {
        fun fromKey(key: String): Reaction? = entries.firstOrNull { it.key == key }
    }
}