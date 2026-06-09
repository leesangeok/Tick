package app.tick.auth

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

/**
 * 카카오 사용자 정보 응답을 정규화한다.
 * Kakao /v2/user/me 응답 구조:
 * {
 *   "id": 12345,
 *   "kakao_account": {
 *     "email": "user@example.com",
 *     "profile": { "nickname": "닉네임" }
 *   }
 * }
 */
@Service
class KakaoOAuth2UserService : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val raw = super.loadUser(userRequest)
        val attributes = raw.attributes
        val kakaoAccount = attributes["kakao_account"] as? Map<*, *>
        val profile = kakaoAccount?.get("profile") as? Map<*, *>

        val normalized = mutableMapOf<String, Any>().apply {
            put("kakaoId", (attributes["id"] as Number).toLong())
            putIfNotNull("email", kakaoAccount?.get("email"))
            putIfNotNull("nickname", profile?.get("nickname"))
            putAll(attributes)
        }
        return DefaultOAuth2User(raw.authorities, normalized, "kakaoId")
    }

    private fun MutableMap<String, Any>.putIfNotNull(key: String, value: Any?) {
        if (value != null) put(key, value)
    }
}
