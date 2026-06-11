package com.jinsupark.helpumta.data.mapper

import com.jinsupark.helpumta.domain.model.Group
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toGroup(): Group? {
    return Group(
        id = id,
        name = getString("name") ?: return null,
        description = getString("description").orEmpty(),
        imageUrl = getString("imageUrl").orEmpty(),
        memberCount = ((get("members") as? List<*>)?.size) ?: 0
    )
}