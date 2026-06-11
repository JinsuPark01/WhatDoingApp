package com.jinsupark.helpumta.data.mapper

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
        createdAt = getLong("createdAt") ?: 0L
    )
}