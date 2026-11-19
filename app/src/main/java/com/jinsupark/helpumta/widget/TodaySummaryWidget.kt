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
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign

/** 오늘 내 작성 현황 + 기록하러 가기 */
class TodaySummaryWidget : GlanceAppWidget() {

    // LocalSize가 실제 할당 크기를 돌려주게 한다.
    // 기본값(Single)은 XML의 minWidth/minHeight를 그대로 보고하므로
    // 짧은 변을 계산하는 쪽에서 엉뚱한 값을 쓰게 된다.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        runCatching { ensureSnapshot(context, id) }

        provideContent {
            val snapshot = WidgetSnapshot.fromJson(currentState<Preferences>()[SNAPSHOT_KEY])
            HelpumtaWidgetTheme { Body(snapshot.toTodayState()) }
        }
    }
}

@Composable
private fun Body(state: TodayState) {
    val context = LocalContext.current

    // 런처가 준 영역은 세로로 길 수 있다(One UI 2x2 칸이 그렇다).
    // 영역 전체를 칠하면 세로로 긴 상자가 되므로, 짧은 변에 맞춰 정사각만 칠한다.
    val size = LocalSize.current
    val side = minOf(size.width, size.height)

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .size(side)
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
                            fontSize = 13.sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = "$done / $total",
                        style = TextStyle(
                            color = if (done == total) {
                                GlanceTheme.colors.onSurface
                            } else {
                                GlanceTheme.colors.primary
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    ActionChip(
                        text = if (done == total) "모두 완료" else "기록하기",
                        enabled = done != total,
                        onClickIntent = intent
                    )
                }
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