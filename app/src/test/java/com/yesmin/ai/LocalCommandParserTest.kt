package com.yesmin.ai
import com.yesmin.ai.commands.LocalCommandParser
import org.junit.Assert.*
import org.junit.Test
class LocalCommandParserTest {
 @Test fun hinglishApp() { val a=LocalCommandParser.parse("Yesmin, Chrome kholo.")!!.actions.single();assertEquals("OPEN_APP",a.type);assertEquals("chrome",a.argument) }
 @Test fun wakeNameLongestMatch() {assertEquals("chrome",LocalCommandParser.parse("YESMIN AI, open Chrome")!!.actions.single().argument)}
 @Test fun youtubeSearch() { val a=LocalCommandParser.parse("YouTube par relaxing music search karo")!!.actions.single();assertEquals("YOUTUBE_SEARCH",a.type);assertEquals("relaxing music",a.argument) }
 @Test fun playOpensSearch() { assertEquals("instrumental music",LocalCommandParser.parse("Play instrumental music on YouTube")!!.actions.single().argument) }
 @Test fun browserMultiStep() { val a=LocalCommandParser.parse("Chrome kholo aur Google par weather search karo")!!.actions.single();assertEquals("WEB_SEARCH",a.type);assertEquals("weather",a.argument) }
 @Test fun youtubeMultiStep() { assertEquals("relaxing piano music",LocalCommandParser.parse("YouTube kholo aur relaxing piano music search karo")!!.actions.single().argument) }
 @Test fun urlPathCasePreserved() { assertEquals("https://example.com/AbCd",LocalCommandParser.parse("Open example.com/AbCd in Chrome")!!.actions.single().argument) }
 @Test fun hindiAppCommand() { assertEquals("chrome",LocalCommandParser.parse("chrome खोलो")!!.actions.single().argument) }
 @Test fun banglaAppCommand() { assertEquals("chrome",LocalCommandParser.parse("chrome খোলো")!!.actions.single().argument) }
 @Test fun preciseTap() { val g=LocalCommandParser.gesture("tap 300,600")!!;assertEquals("TAP",g.kind);assertEquals(300f,g.x,0f) }
 @Test fun gestureMustBeExact() {assertNull(LocalCommandParser.gesture("Please read the words swipe up")); assertNull(LocalCommandParser.gesture("tap the payment button"))}
 @Test fun tapRejectsNegative() {assertNull(LocalCommandParser.gesture("tap -1,400"))}
 @Test fun noShellParser() {assertNull(LocalCommandParser.parse("rm -rf /"))}
 @Test fun chromeWebsiteAlias() {assertEquals("https://www.wikipedia.org",LocalCommandParser.parse("Chrome mein Wikipedia open karo")!!.actions.single().argument)}
 @Test fun settingsHinglish() {assertEquals("settings",LocalCommandParser.parse("Settings mein jao")!!.actions.single().argument)}
 @Test fun youtubeWebsiteRequest() {assertEquals("https://www.youtube.com",LocalCommandParser.parse("YouTube website par jao")!!.actions.single().argument)}
 @Test fun unsupportedMultiStepFallsBackToGemini() {assertNull(LocalCommandParser.parse("Chrome kholo aur explain a poem"))}
 @Test fun naturalChatFallsBackToGemini() {assertNull(LocalCommandParser.parse("Explain recursion in Python"))}
}
