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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign

/** 오늘 내 작성 현황 + 기록하러 가기 */
class TodaySummaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 조회·상태 기록 실패가 시스템 오류 화면("정보를 불러올 수 없습니다")으로
        // 번지지 않게 한다. 실패해도 아래에서 우리 문구를 보여주고 다음 갱신 때 재시도한다.
        runCatching { ensureSnapshot(context, id) }

        provideContent {
            val snapshot = WidgetSnapshot.fromJson(currentState<Preferences>()[SNAPSHOT_KEY])
            GlanceTheme { Body(snapshot.toTodayState()) }
        }
    }
}

@Composable
private fun Body(state: TodayState) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            TodayState.LoggedOut -> Message("로그인이 필요해요")
            TodayState.NoGroups -> Message("그룹을 만들어보세요")
            TodayState.Error -> Message("불러오지 못했어요")
            is TodayState.Content -> {
                val done = state.groups.count { it.didIWrite }
                val total = state.groups.size
                val remaining = state.groups.filter { !it.didIWrite }

                // 남은 그룹이 하나면 거기로 바로 보낸다.
                // 여러 개면 어디로 갈지 고를 수 없으니 홈으로.
                val intent = if (remaining.size == 1) {
                    groupIntent(remaining.first().groupId)
                } else {
                    mainActivityIntent(context)
                }

                Text(
                    text = "오늘",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = "$done / $total",
                    style = TextStyle(
                        color = if (done == total) {
                            GlanceTheme.colors.onSurface
                        } else {
                            GlanceTheme.colors.primary
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

                ActionChip(
                    text = if (done == total) "모두 완료" else "기록하기",
                    enabled = done != total,
                    onClickIntent = intent
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    enabled: Boolean,
    onClickIntent: android.content.Intent
) {
    // Glance의 Button은 버전에 따라 패키지가 달라서 Box로 직접 만든다
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                if (enabled) GlanceTheme.colors.primary
                else GlanceTheme.colors.surfaceVariant
            )
            .cornerRadius(10.dp)
            .padding(vertical = 8.dp)
            .clickable(actionStartActivity(onClickIntent)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = if (enabled) GlanceTheme.colors.onPrimary
                else GlanceTheme.colors.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun Message(text: String) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(mainActivityIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}