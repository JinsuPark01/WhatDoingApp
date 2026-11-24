# ─────────────────────────────────────────────
# 헬품타 R8 규칙
# 대부분의 라이브러리(Firebase, Hilt, Compose, Coil, Glance, DataStore)는
# 자체 규칙(consumer rules)을 AAR에 포함하고 있어 여기서 따로 적지 않는다.
# 여기는 "우리 코드가 이름에 기대는 곳"과 "공식 문서가 직접 추가하라고 한 것"만 둔다.
# ─────────────────────────────────────────────

# ── 크래시 스택 트레이스에 줄 번호 유지
# Play Console이 AAB에 포함된 mapping 파일로 이름을 되돌려주는데,
# 줄 번호까지 있어야 어느 줄에서 터졌는지 알 수 있다.
# 원본 파일 이름은 숨긴다 (줄 번호만 있어도 mapping으로 충분)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── 저장소에 이름(name)으로 저장하는 도메인 enum
# ExtractTemplate / ExtractRatio / ExtractColor는 DataStore에 enum.name 그대로 저장하고
# valueOf로 되읽는다. 이름이 바뀌면 업데이트 후 저장값을 못 읽어 기본값으로 돌아간다.
# (Reaction은 별도 key 문자열을 쓰지만 같은 패키지라 함께 보존 — 비용은 무시할 수준)
-keepclassmembers enum com.jinsupark.helpumta.domain.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Credential Manager (구글 로그인)
# Play 서비스 구현체를 리플렉션으로 찾는다. 공식 문서가 직접 추가하라고 안내하는 규칙.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}