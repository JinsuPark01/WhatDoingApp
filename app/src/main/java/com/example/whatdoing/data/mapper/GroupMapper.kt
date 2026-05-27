package com.example.whatdoing.data.mapper

import com.example.whatdoing.domain.model.Group
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