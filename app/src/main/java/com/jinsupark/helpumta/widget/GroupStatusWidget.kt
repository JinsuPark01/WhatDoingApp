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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 그룹별 오늘 작성 현황 */
class GroupStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 조회·상태 기록 실패가 시스템 오류 화면("정보를 불러올 수 없습니다")으로
        // 번지지 않게 한다. 실패해도 아래에서 우리 문구를 보여주고 다음 갱신 때 재시도한다.
        runCatching { ensureSnapshot(context, id) }

        provideContent {
            val snapshot = WidgetSnapshot.fromJson(currentState<Preferences>()[SNAPSHOT_KEY])
            HelpumtaWidgetTheme { Body(snapshot.toTodayState()) }
        }
    }
}
@Composable
private fun Body(state: TodayState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Header()
        Spacer(modifier = GlanceModifier.height(8.dp))

        when (state) {
            TodayState.LoggedOut -> CenterMessage("로그인이 필요해요")
            TodayState.NoGroups -> CenterMessage("참여 중인 그룹이 없어요")
            TodayState.Error -> CenterMessage("불러오지 못했어요")
            is TodayState.Content -> GroupList(state.groups)
        }
    }
}

@Composable
private fun Header() {
    val context = LocalContext.current
    val date = SimpleDateFormat("M월 d일", Locale.KOREA).format(Date())

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(mainActivityIntent(context))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "헬품타",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = date,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun GroupList(groups: List<GroupStatus>) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(groups, itemId = { it.groupId.hashCode().toLong() }) { group ->
            GroupRow(group)
        }
    }
}

@Composable
private fun GroupRow(group: GroupStatus) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(actionStartActivity(groupIntent(group.groupId))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.name,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        Text(
            text = if (group.didIWrite) "완료" else "미작성",
            style = TextStyle(
                color = if (group.didIWrite) {
                    GlanceTheme.colors.onSurfaceVariant
                } else {
                    GlanceTheme.colors.primary
                },
                fontSize = 12.sp,
                fontWeight = if (group.didIWrite) FontWeight.Normal else FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        Text(
            text = "${group.wroteCount}/${group.memberCount}",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun CenterMessage(message: String) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(mainActivityIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 13.sp
            )
        )
    }
}