package com.jinsupark.helpumta.domain.util

import java.util.Calendar

/** 오늘 00:00 (기기 시간대) */
fun startOfToday(): Long =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/**
 * 86_400_000L을 더하지 않고 Calendar를 쓰는 이유는 서머타임 때문이다.
 * 한국은 해당 없지만, 하루의 길이를 상수로 가정하는 코드는 기기 시간대가 바뀌면 조용히 틀린다.
 */
fun addDays(millis: Long, days: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis

/** 월=0 ... 일=6 */
fun dayIndexInWeek(millis: Long): Int {
    val dow = Calendar.getInstance().apply { timeInMillis = millis }
        .get(Calendar.DAY_OF_WEEK)
    return (dow + 5) % 7
}