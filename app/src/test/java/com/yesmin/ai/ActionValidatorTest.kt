package com.yesmin.ai
import com.yesmin.ai.commands.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
class ActionValidatorTest {
 @Test fun publicHttpsAllowed() { assertTrue(ActionValidator.validUrl("https://en.wikipedia.org/wiki/Kotlin")) }
 @Test fun httpRejected() { assertFalse(ActionValidator.validUrl("http://example.com")) }
 @Test fun executableSchemesRejected() { listOf("javascript:alert(1)","intent://test","file:///sdcard/test","content://secret","tel:123").forEach { assertFalse(ActionValidator.validUrl(it)) } }
 @Test fun credentialsRejected() { assertFalse(ActionValidator.validUrl("https://user:pass@example.com")) }
 @Test fun localTargetsRejected() { listOf("https://localhost","https://127.0.0.1","https://192.168.1.1","https://printer.local","https://company.internal").forEach {assertFalse(ActionValidator.validUrl(it))} }
 @Test fun ipv6LocalLiteralRejected() {assertFalse(ActionValidator.validUrl("https://[::ffff:192.168.1.1]/"))}
 @Test fun localhostSubdomainRejected() {assertFalse(ActionValidator.validUrl("https://app.localhost/"))}
 @Test fun customPortRejected() { assertFalse(ActionValidator.validUrl("https://example.com:8443")) }
 @Test fun whitespaceRejected() { assertFalse(ActionValidator.validUrl("https://example.com/a b")) }
 @Test fun knownActionsAllowed() { assertTrue(ActionValidator.validate(listOf(ProposedAction("OPEN_APP","Chrome"),ProposedAction("YOUTUBE_SEARCH","relaxing piano"))).isSuccess) }
 @Test fun shellRejected() { assertTrue(ActionValidator.validate(listOf(ProposedAction("SHELL","rm -rf /"))).isFailure) }
 @Test fun aiGesturesRejected() { assertTrue(ActionValidator.validate(listOf(ProposedAction("TAP","300,600"))).isFailure) }
 @Test fun emptyArgumentRejected() { assertTrue(ActionValidator.validate(listOf(ProposedAction("OPEN_APP",""))).isFailure) }
 @Test fun tooManyActionsRejected() { assertTrue(ActionValidator.validate(List(4){ProposedAction("OPEN_APP","Chrome")}).isFailure) }
 @Test fun extraJsonFieldsRejected() { assertTrue(runCatching { Json.decodeFromString<BrainReply>("""{"reply":"hello","actions":[],"shell":"echo test"}""") }.isFailure) }
 @Test fun strictJsonAcceptsCode() { val r=Json.decodeFromString<BrainReply>("""{"reply":"```kotlin\nval x = 1\n```","actions":[]}"""); assertTrue(r.reply.contains("val x")) }
}
