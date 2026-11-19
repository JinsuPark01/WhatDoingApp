package com.jinsupark.helpumta.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

private val WEEK_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 이번 주 내 기록 현황 */
class WeeklyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        runCatching { ensureSnapshot(context, id) }

        provideContent {
            val snapshot = WidgetSnapshot.fromJson(currentState<Preferences>()[SNAPSHOT_KEY])
            HelpumtaWidgetTheme { Body(snapshot.toWeekState()) }
        }
    }
}

@Composable
private fun Body(state: WeekState) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(mainActivityIntent(context)))
    ) {
        when (state) {
            WeekState.LoggedOut -> Message("로그인이 필요해요")
            WeekState.NoGroups -> Message("참여 중인 그룹이 없어요")
            WeekState.Error -> Message("불러오지 못했어요")
            is WeekState.Content -> {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이번 주",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "${state.doneDays.size}일",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    WEEK_LABELS.forEachIndexed { index, label ->
                        // defaultWeight()는 RowScope 멤버라 여기(Row 블록 안)에서만 쓸 수 있다.
                        // DayColumn 안에서는 스코프를 벗어나므로 modifier로 넘긴다.
                        DayColumn(
                            label = label,
                            done = state.doneDays.contains(index),
                            isFuture = index > state.todayIndex,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    label: String,
    done: Boolean,
    isFuture: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = GlanceModifier.height(3.dp))

        Text(
            // ● 기록함 / ○ 안 함 / · 아직 안 온 날
            text = when {
                done -> "●"
                isFuture -> "·"
                else -> "○"
            },
            style = TextStyle(
                color = if (done) {
                    GlanceTheme.colors.primary
                } else {
                    GlanceTheme.colors.onSurfaceVariant
                },
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun Message(text: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp
            )
        )
    }
}