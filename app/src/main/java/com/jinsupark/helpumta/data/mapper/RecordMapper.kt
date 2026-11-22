package com.jinsupark.helpumta.data.mapper

import com.jinsupark.helpumta.domain.model.Reaction
import com.jinsupark.helpumta.domain.model.WorkoutRecord
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toWorkoutRecord(): WorkoutRecord? {
    return WorkoutRecord(
        id = id,
        groupId = getString("groupId") ?: return null,
        userId = getString("userId") ?: return null,
        userName = getString("userName").orEmpty(),
        workoutType = getString("workoutType").orEmpty(),
        workoutDuration = getLong("workoutDuration")?.toInt() ?: 0,
        imageUrl = getString("imageUrl").orEmpty(),
        comment = getString("comment").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L,
        reactions = parseReactions(get("reactions"))
    )
}

// 모르는 key(구버전 앱이 못 읽는 새 종류 등)는 버린다 — 기록 자체를 못 읽게 만들지 않도록
private fun parseReactions(raw: Any?): Map<String, Reaction> {
    val map = raw as? Map<*, *> ?: return emptyMap()
    return map.entries.mapNotNull { (uid, key) ->
        val userId = uid as? String ?: return@mapNotNull null
        val reaction = (key as? String)?.let { Reaction.fromKey(it) } ?: return@mapNotNull null
        userId to reaction
    }.toMap()
}